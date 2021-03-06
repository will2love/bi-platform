/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.rigel.biplatform.ma.rt.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ma.report.exception.QueryModelBuildException;
import com.baidu.rigel.biplatform.ma.report.model.ExtendArea;
import com.baidu.rigel.biplatform.ma.report.model.Item;
import com.baidu.rigel.biplatform.ma.report.model.LiteOlapExtendArea;
import com.baidu.rigel.biplatform.ma.report.model.LogicModel;
import com.baidu.rigel.biplatform.ma.report.model.ReportDesignModel;
import com.baidu.rigel.biplatform.ma.report.utils.QueryUtils;
import com.baidu.rigel.biplatform.ma.rt.Context;
import com.baidu.rigel.biplatform.ma.rt.ExtendAreaContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 工具类：用于提供运行时环境初始化、运行时上下文操作等
 *
 * @author david.wang
 * @version 1.0.0.1
 */
public final class RuntimeEvnUtil {
    /**
     * 日期
     */
    private static Logger logger = Logger.getLogger(RuntimeEvnUtil.class);
	/**
	 * 构造函数
	 */
	private RuntimeEvnUtil () {
	}
	
	/**
	 * 内部上下文类，包含全局上下文context，以及区域上下文列表
	 */
	public class ContextEntity {
	    /**
	     * 全局上下文
	     */
	    private Context context;
	    /**
	     * 区域上下文列表
	     */
	    private List<ExtendAreaContext> extendAreaContextLists = Lists.newArrayList();
	    /**
	     * 
	     * 构造函数
	     */
	    public ContextEntity() {
	        
	    }
	    /**
	     * 
	     * getContext
	     * @return
	     */
        public Context getContext() {           
            return context;            
        }
        /**
         * 
         * setContext
         * @param context
         */
        public void setContext(Context context) {           
            this.context = context;           
        }
        /**
         * 
         * getExtendAreaContextLists
         * @return
         */
        public List<ExtendAreaContext> getExtendAreaContextLists() {            
            return extendAreaContextLists;           
        }
        /**
         * 
         * setExtendAreaContextLists
         * @param extendAreaContextLists
         */
        public void setExtendAreaContextLists(List<ExtendAreaContext> extendAreaContextLists) {            
            this.extendAreaContextLists = extendAreaContextLists;          
        }
	}
	/**
	 * 根据报表id初始化报表对应运行时啥下文
	 * @param designModel 报表模型
	 * @return Context 运行时上下文
	 */
	public static final ContextEntity initRuntimeEvn(ReportDesignModel designModel, ApplicationContext applicationContext) {
	    ContextEntity contextEntity = new RuntimeEvnUtil().new ContextEntity();
	    // 上下文信息
	    Context context = new Context(applicationContext);
	    contextEntity.setContext(context);
	    
	    // 局部上下文参数
	    List<ExtendAreaContext> extendAreaContextLists = Lists.newArrayList();
	    // 获取扩展区域列表
	    ExtendArea[] extendAreas = designModel.getExtendAreaList();
	    if (extendAreas != null) {
	        // 遍历扩展区，获取每个扩展区的上下文信息
	        for (ExtendArea extendArea : extendAreas) {
	            ExtendAreaContext localContext = getLocalContextOfExtendArea(extendArea, designModel);
	            extendAreaContextLists.add(localContext);
	        }
	    }
	    contextEntity.setExtendAreaContextLists(extendAreaContextLists);
		return contextEntity;
	}
	
	/**
	 * 获取扩展区域上下文
	 * getLocalContextOfExtendArea
	 * @param extendArea
	 * @return context 局部上下文
	 */
	private static ExtendAreaContext getLocalContextOfExtendArea (ExtendArea extendArea, ReportDesignModel designModel) {
	    ExtendAreaContext context = new ExtendAreaContext();
	    context.setAreaId(extendArea.getId());
	    context.setAreaType(extendArea.getType());
	    context.setReportId(designModel.getId());
	    // 设置数据格式模型
	    context.setFormatModel(extendArea.getFormatModel());
	    // 设置cube定义
        try {
            Cube cubeDefine = QueryUtils.getCubeWithExtendArea(designModel, extendArea);
            context.setCubeDefine(cubeDefine);
        } catch (QueryModelBuildException e) {
            logger.error("fail to get cube define", e);            
        }
	    // 获取区域逻辑模型
	    LogicModel logicModel = extendArea.getLogicModel();
	    // 利用逻辑模型获取行轴，列轴，以及切片轴信息
	    Item[] columns = logicModel.getColumns();
	    Item[] rows = logicModel.getRows();
	    Item[] slices = logicModel.getSlices();
	    LinkedHashMap<Item, Object> x = Maps.newLinkedHashMap();
	    for (Item item : rows) {
	        x.put(item, null);
	    }
	    LinkedHashMap<Item, Object> y = Maps.newLinkedHashMap();
        for (Item item : columns) {
            y.put(item, null);
        }
	    LinkedHashMap<Item, Object> s = Maps.newLinkedHashMap();
        for (Item item : slices) {
            s.put(item, null);
        } 
        // 将行轴，列轴，切片轴信息添加到上下文中
        context.setX(x);
        context.setY(y);
        context.setS(s);
        
	    switch (extendArea.getType()) {
	        case LITEOLAP:
	            LiteOlapExtendArea liteOlapExtendArea = (LiteOlapExtendArea) extendArea;
	            // 获取候选维度，候选指标
	            Map<String, Item> canDims = liteOlapExtendArea.getCandDims();
	            Map<String, Item> canInds = liteOlapExtendArea.getCandInds();
	            context.setCanDim((Set<Item>) canDims.values());
	            context.setCanInd((Set<Item>) canInds.values());
	        default:
	    }
	    return context;
	}
}
