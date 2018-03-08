package com.tierconnect.riot.iot.utils.rest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by pablo on 6/16/16.
 *
 * Same as UrlEncodedFormEntity, but it passes the parameters separated by a comma instead of '&'.
 * This is used only on the submit of jobs to the spark server
 */
public class TextEncodedFormEntity extends StringEntity {
//    public TextEncodedFormEntity(List<? extends NameValuePair> parameters, String charset)
//            throws UnsupportedEncodingException {
//        super(
//                format(
//                        parameters,
//                        ',',
//                         charset != null ? charset : HTTP.DEF_CONTENT_CHARSET.name()
//                ),
//                ContentType.create(URLEncodedUtils.CONTENT_TYPE, charset)
//        );
//    }

    public TextEncodedFormEntity(Iterable<? extends NameValuePair> parameters, Charset charset) {
        super(format(parameters,
                ',',
                charset != null ? charset : HTTP.DEF_CONTENT_CHARSET),
                ContentType.create(URLEncodedUtils.CONTENT_TYPE, charset));
    }

    public TextEncodedFormEntity(List<? extends NameValuePair> parameters) throws UnsupportedEncodingException {
        this(parameters, (Charset) null);
    }

    public TextEncodedFormEntity(Iterable<? extends NameValuePair> parameters) {
        this(parameters, null);
    }


//    private static String format(
//            final List <? extends NameValuePair> parameters,
//            final char parameterSeparator,
//            final String charset) {
//        return format(parameters, parameterSeparator, charset);
//    }

    public static String format(
            final Iterable<? extends NameValuePair> parameters,
            final char parameterSeparator,
            final Charset charset) {
        final StringBuilder result = new StringBuilder();
        for (final NameValuePair parameter : parameters) {
            final String encodedName = parameter.getName();
            final String encodedValue = parameter.getValue();
            if (result.length() > 0) {
                result.append(parameterSeparator);
            }
            result.append(encodedName);
            if (encodedValue != null) {
                result.append("=");
                result.append(encodedValue);
            }
        }
        return result.toString();
    }
}
