package com.dotcms.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.ws.rs.core.GenericEntity;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value.Immutable
@Value.Style(
        typeAbstract = "Abstract*",
        typeImmutable = "*",
        visibility = Value.Style.ImplementationVisibility.PUBLIC)
@JsonSerialize
@JsonDeserialize(builder = GenericResponseEntityView.Builder.class)
public interface AbstractGenericResponseEntityView<T> {
    String OK = "Ok";

    List<ErrorEntity> errors();

    T entity();


    List<MessageEntity> messages();
    Map<String, String> i18nMessagesMap();
    List<String> permissions();
}
