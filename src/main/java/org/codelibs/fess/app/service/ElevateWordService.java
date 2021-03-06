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
package org.codelibs.fess.app.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.codelibs.core.beans.util.BeanUtil;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.pager.ElevateWordPager;
import org.codelibs.fess.es.config.cbean.ElevateWordCB;
import org.codelibs.fess.es.config.exbhv.ElevateWordBhv;
import org.codelibs.fess.es.config.exbhv.ElevateWordToLabelBhv;
import org.codelibs.fess.es.config.exentity.ElevateWord;
import org.codelibs.fess.es.config.exentity.ElevateWordToLabel;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.bhv.readable.EntityRowHandler;
import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.optional.OptionalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orangesignal.csv.CsvConfig;
import com.orangesignal.csv.CsvReader;
import com.orangesignal.csv.CsvWriter;

public class ElevateWordService implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ElevateWordService.class);

    @Resource
    protected ElevateWordToLabelBhv elevateWordToLabelBhv;

    @Resource
    protected ElevateWordBhv elevateWordBhv;

    @Resource
    protected FessConfig fessConfig;

    public List<ElevateWord> getElevateWordList(final ElevateWordPager elevateWordPager) {

        final PagingResultBean<ElevateWord> elevateWordList = elevateWordBhv.selectPage(cb -> {
            cb.paging(elevateWordPager.getPageSize(), elevateWordPager.getCurrentPageNumber());
            setupListCondition(cb, elevateWordPager);
        });

        // update pager
        BeanUtil.copyBeanToBean(elevateWordList, elevateWordPager, option -> option.include(Constants.PAGER_CONVERSION_RULE));
        elevateWordPager.setPageNumberList(elevateWordList.pageRange(op -> {
            op.rangeSize(5);
        }).createPageNumberList());

        return elevateWordList;
    }

    public OptionalEntity<ElevateWord> getElevateWord(final String id) {
        return elevateWordBhv.selectByPK(id).map(entity -> {

            final List<ElevateWordToLabel> wctltmList = elevateWordToLabelBhv.selectList(wctltmCb -> {
                wctltmCb.query().setElevateWordId_Equal(entity.getId());
                wctltmCb.fetchFirst(fessConfig.getPageLabeltypeMaxFetchSizeAsInteger());
            });
            if (!wctltmList.isEmpty()) {
                final List<String> labelTypeIds = new ArrayList<String>(wctltmList.size());
                for (final ElevateWordToLabel mapping : wctltmList) {
                    labelTypeIds.add(mapping.getLabelTypeId());
                }
                entity.setLabelTypeIds(labelTypeIds.toArray(new String[labelTypeIds.size()]));
            }
            return entity;
        });
    }

    public void store(final ElevateWord elevateWord) {
        final boolean isNew = elevateWord.getId() == null;
        final String[] labelTypeIds = elevateWord.getLabelTypeIds();

        elevateWordBhv.insertOrUpdate(elevateWord, op -> {
            op.setRefresh(true);
        });
        final String elevateWordId = elevateWord.getId();
        if (isNew) {
            // Insert
            if (labelTypeIds != null) {
                final List<ElevateWordToLabel> wctltmList = new ArrayList<ElevateWordToLabel>();
                for (final String id : labelTypeIds) {
                    final ElevateWordToLabel mapping = new ElevateWordToLabel();
                    mapping.setElevateWordId(elevateWordId);
                    mapping.setLabelTypeId(id);
                    wctltmList.add(mapping);
                }
                elevateWordToLabelBhv.batchInsert(wctltmList, op -> {
                    op.setRefresh(true);
                });
            }
        } else {
            // Update
            if (labelTypeIds != null) {
                final List<ElevateWordToLabel> list = elevateWordToLabelBhv.selectList(wctltmCb -> {
                    wctltmCb.query().setElevateWordId_Equal(elevateWordId);
                    wctltmCb.fetchFirst(fessConfig.getPageLabeltypeMaxFetchSizeAsInteger());
                });
                final List<ElevateWordToLabel> newList = new ArrayList<ElevateWordToLabel>();
                final List<ElevateWordToLabel> matchedList = new ArrayList<ElevateWordToLabel>();
                for (final String id : labelTypeIds) {
                    boolean exist = false;
                    for (final ElevateWordToLabel mapping : list) {
                        if (mapping.getLabelTypeId().equals(id)) {
                            exist = true;
                            matchedList.add(mapping);
                            break;
                        }
                    }
                    if (!exist) {
                        // new
                        final ElevateWordToLabel mapping = new ElevateWordToLabel();
                        mapping.setElevateWordId(elevateWordId);
                        mapping.setLabelTypeId(id);
                        newList.add(mapping);
                    }
                }
                list.removeAll(matchedList);
                elevateWordToLabelBhv.batchInsert(newList, op -> {
                    op.setRefresh(true);
                });
                elevateWordToLabelBhv.batchDelete(list, op -> {
                    op.setRefresh(true);
                });
            }
        }
    }

    public void delete(final ElevateWord elevateWord) {

        elevateWordBhv.delete(elevateWord, op -> {
            op.setRefresh(true);
        });

    }

    protected void setupListCondition(final ElevateWordCB cb, final ElevateWordPager elevateWordPager) {
        if (elevateWordPager.id != null) {
            cb.query().docMeta().setId_Equal(elevateWordPager.id);
        }
        // TODO Long, Integer, String supported only.

        // setup condition
        cb.query().addOrderBy_SuggestWord_Asc();

        // search

    }

    public void importCsv(final Reader reader) {
        @SuppressWarnings("resource")
        final CsvReader csvReader = new CsvReader(reader, new CsvConfig());
        try {
            List<String> list;
            csvReader.readValues(); // ignore header
            while ((list = csvReader.readValues()) != null) {
                final String suggestWord = getValue(list, 0);
                if (StringUtil.isBlank(suggestWord)) {
                    // skip
                    continue;
                }
                try {
                    final String role = getValue(list, 2);
                    final String label = getValue(list, 3);
                    ElevateWord elevateWord = elevateWordBhv.selectEntity(cb -> {
                        cb.query().setSuggestWord_Equal(suggestWord);
                        if (StringUtil.isNotBlank(role)) {
                            cb.query().setTargetRole_Equal(role);
                        }
                        if (StringUtil.isNotBlank(label)) {
                            cb.query().setTargetLabel_Equal(label);
                        }
                    }).orElse(null);//TODO
                    final String reading = getValue(list, 1);
                    final String boost = getValue(list, 4);
                    final long now = ComponentUtil.getSystemHelper().getCurrentTimeAsLong();
                    if (elevateWord == null) {
                        elevateWord = new ElevateWord();
                        elevateWord.setSuggestWord(suggestWord);
                        elevateWord.setReading(reading);
                        elevateWord.setTargetRole(role);
                        elevateWord.setTargetLabel(label);
                        elevateWord.setBoost(StringUtil.isBlank(boost) ? 1.0f : Float.parseFloat(boost));
                        elevateWord.setCreatedBy("system");
                        elevateWord.setCreatedTime(now);
                        elevateWordBhv.insert(elevateWord);
                    } else if (StringUtil.isBlank(reading) && StringUtil.isBlank(boost)) {
                        elevateWordBhv.delete(elevateWord);
                    } else {
                        elevateWord.setReading(reading);
                        elevateWord.setBoost(StringUtil.isBlank(boost) ? 1.0f : Float.parseFloat(boost));
                        elevateWord.setUpdatedBy("system");
                        elevateWord.setUpdatedTime(now);
                        elevateWordBhv.update(elevateWord);
                    }
                } catch (final Exception e) {
                    logger.warn("Failed to read a sugget elevate word: " + list, e);
                }
            }
        } catch (final IOException e) {
            logger.warn("Failed to read a sugget elevate word.", e);
        }
    }

    public void exportCsv(final Writer writer) {
        final CsvConfig cfg = new CsvConfig(',', '"', '"');
        cfg.setEscapeDisabled(false);
        cfg.setQuoteDisabled(false);
        @SuppressWarnings("resource")
        final CsvWriter csvWriter = new CsvWriter(writer, cfg);
        try {
            final List<String> list = new ArrayList<String>();
            list.add("SuggestWord");
            list.add("Reading");
            list.add("Role");
            list.add("Label");
            list.add("Boost");
            csvWriter.writeValues(list);

            elevateWordBhv.selectCursor(cb -> {
                cb.query().matchAll();
            }, new EntityRowHandler<ElevateWord>() {
                @Override
                public void handle(final ElevateWord entity) {
                    final List<String> list = new ArrayList<String>();
                    addToList(list, entity.getSuggestWord());
                    addToList(list, entity.getReading());
                    addToList(list, entity.getTargetRole());
                    addToList(list, entity.getTargetLabel());
                    addToList(list, entity.getBoost());
                    try {
                        csvWriter.writeValues(list);
                    } catch (final IOException e) {
                        logger.warn("Failed to write a sugget elevate word: " + entity, e);
                    }
                }

                private void addToList(final List<String> list, final Object value) {
                    if (value == null) {
                        list.add(StringUtil.EMPTY);
                    } else {
                        list.add(value.toString());
                    }
                }
            });

            csvWriter.flush();
        } catch (final IOException e) {
            logger.warn("Failed to write a sugget elevate word.", e);
        }
    }

    static String getValue(final List<String> list, final int index) {
        if (index >= list.size()) {
            return StringUtil.EMPTY;
        }
        String item = list.get(index).trim();
        if (StringUtil.isBlank(item)) {
            return StringUtil.EMPTY;
        }
        if (item.length() > 1 && item.charAt(0) == '"' && item.charAt(item.length() - 1) == '"') {
            item = item.substring(1, item.length() - 1);
        }
        return item;
    }

}
