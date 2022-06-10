package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value.Immutable
@Value.Style(
        typeAbstract = "Abstract*",
        typeImmutable = "*",
        visibility = Value.Style.ImplementationVisibility.PUBLIC)
@JsonSerialize
@JsonDeserialize(builder = ListResponseEntityView.Builder.class)
public interface AbstractListResponseEntityView<T> extends AbstractGenericResponseEntityView<List<T>>{


}
