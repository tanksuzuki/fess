/*
 * Copyright 2012-2016 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.app.web.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Size;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.entity.FacetInfo;
import org.codelibs.fess.entity.GeoInfo;
import org.codelibs.fess.entity.SearchRequestParams;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.StreamUtil;
import org.lastaflute.web.validation.theme.conversion.ValidateTypeFailure;

public class SearchForm implements SearchRequestParams, Serializable {

    private static final long serialVersionUID = 1L;

    public Map<String, String[]> fields = new HashMap<>();

    @Size(max = 1000)
    public String q;

    @Size(max = 1000)
    public String sort;

    @ValidateTypeFailure
    public Integer num;

    public String[] lang;

    public String ex_q[];

    @ValidateTypeFailure
    public Integer start;

    @ValidateTypeFailure
    public Integer pn;

    // response redirect

    // geo

    public GeoInfo geo;

    // facet

    public FacetInfo facet;

    // advance

    @Override
    public int getStartPosition() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        if (start == null) {
            start = fessConfig.getPagingSearchPageStartAsInteger();
        }
        return start;
    }

    @Override
    public int getPageSize() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        if (num == null) {
            num = fessConfig.getPagingSearchPageSizeAsInteger();
        } else {
            try {
                if (num.intValue() > fessConfig.getPagingSearchPageMaxSizeAsInteger().intValue() || num.intValue() <= 0) {
                    num = fessConfig.getPagingSearchPageMaxSizeAsInteger();
                }
            } catch (final NumberFormatException e) {
                num = fessConfig.getPagingSearchPageSizeAsInteger();
            }
        }
        return num;
    }

    @Override
    public String getQuery() {
        return q;
    }

    @Override
    public String[] getExtraQueries() {
        return StreamUtil.of(ex_q).filter(q -> StringUtil.isNotBlank(q)).distinct().toArray(n -> new String[n]);
    }

    @Override
    public Map<String, String[]> getFields() {
        return fields;
    }

    @Override
    public String[] getLanguages() {
        return StreamUtil.of(lang).filter(q -> StringUtil.isNotBlank(q)).distinct().toArray(n -> new String[n]);
    }

    @Override
    public GeoInfo getGeoInfo() {
        return geo;
    }

    @Override
    public FacetInfo getFacetInfo() {
        return facet;
    }

    @Override
    public String getSort() {
        return sort;
    }

    @Override
    public boolean isAdministrativeAccess() {
        return false;
    }

}
