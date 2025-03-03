import { combineLatest, Observable, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { switchMap, take, takeUntil } from 'rxjs/operators';

import { DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    EDITOR_MARKETING_KEYS
} from '@dotcms/dotcms-models';

export interface BlockEditorInput {
    content: { [key: string]: string };
    fieldName: string;
    language: number;
    inode: string;
    fieldVariables?: {
        allowedBlocks?: string;
        allowedContentTypes?: string;
        styles: string;
    };
}

@Component({
    selector: 'dot-block-editor-sidebar',
    templateUrl: './dot-block-editor-sidebar.component.html',
    styleUrls: ['./dot-block-editor-sidebar.component.scss']
})
export class DotBlockEditorSidebarComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    blockEditorInput: BlockEditorInput;
    showVideoThumbnail: boolean;
    saving = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotEventsService: DotEventsService,
        private dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotContentTypeService: DotContentTypeService,
        private dotPropertiesService: DotPropertiesService
    ) {}

    ngOnInit(): void {
        const content$ = this.dotEventsService.listen<HTMLDivElement>('edit-block-editor').pipe(
            takeUntil(this.destroy$),
            switchMap((event) => this.extractBlockEditorData(event.data.dataset))
        );

        const propery$ = this.dotPropertiesService.getKey(
            EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL
        );

        combineLatest([content$, propery$]).subscribe(([eventData, property = 'true']) => {
            this.blockEditorInput = eventData;
            this.showVideoThumbnail = property === 'true' || property === 'NOT_FOUND';
        });
    }

    /**
     *  Execute the workflow to save the editor changes and then close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    saveEditorChanges(): void {
        this.saving = true;
        this.dotWorkflowActionsFireService
            .saveContentlet({
                [this.blockEditorInput.fieldName]: JSON.stringify(
                    this.blockEditor.editor.getJSON()
                ),
                inode: this.blockEditorInput.inode,
                indexPolicy: 'WAIT_FOR'
            })
            .pipe(take(1))
            .subscribe(
                () => {
                    this.saving = false;
                    const customEvent = new CustomEvent('ng-event', {
                        detail: { name: 'in-iframe' }
                    });
                    window.top.document.dispatchEvent(customEvent);
                    this.closeSidebar();
                },
                (e: HttpErrorResponse) => {
                    this.saving = false;
                    this.dotAlertConfirmService.alert({
                        accept: () => {
                            this.closeSidebar();
                        },
                        header: this.dotMessageService.get('error'),
                        message:
                            e.error?.message || this.dotMessageService.get('editpage.inline.error')
                    });
                }
            );
    }

    /**
     *  Clear the date to close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    closeSidebar(): void {
        this.blockEditorInput = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private extractBlockEditorData(dataSet: {
        [key: string]: string;
    }): Observable<BlockEditorInput> {
        return this.dotContentTypeService.getContentType(dataSet.contentType).pipe(
            switchMap((contentType) =>
                contentType?.fields.filter((field) => field.variable == dataSet.fieldName)
            ),
            switchMap((field: DotCMSContentTypeField) => {
                return of({
                    fieldVariables: this.parseFieldVariables(field.fieldVariables),
                    fieldName: dataSet.fieldName,
                    language: parseInt(dataSet.language),
                    inode: dataSet.inode,
                    content: JSON.parse(dataSet.blockEditorContent),
                    contentletIdentifier: dataSet.contentletIdentifier
                });
            })
        );
    }

    private parseFieldVariables(
        fieldVariables: DotCMSContentTypeFieldVariable[]
    ): BlockEditorInput['fieldVariables'] {
        return {
            allowedBlocks: fieldVariables.find((variable) => variable.key === 'allowedBlocks')
                ?.value,
            allowedContentTypes: fieldVariables.find(
                (variable) => variable.key === 'allowedContentTypes'
            )?.value,
            styles: fieldVariables.find((variable) => variable.key === 'styles')?.value
        };
    }
}
