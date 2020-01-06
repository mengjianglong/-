package com.aisile.search.service.impl;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Crotch;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;

import com.alibaba.dubbo.config.annotation.Service;
import com.aisile.pojo.TbItem;
import com.aisile.search.service.*;
@Service(timeout=3000)
public class ItemSearchServiceImpl implements ItemSearchService{
	@Autowired
	private SolrTemplate solrTemplate;
	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public Map<String, Object> search(Map searchMap) {
		Map<String,Object> map=new HashMap<>();
		//关键字空格处理 
		String keywords = (String) searchMap.get("keywords");
		searchMap.put("keywords", keywords.replace(" ",""));
		map.putAll(searchList(searchMap));
		List categoryList = searchCategoryList(searchMap);
		map.put("categoryList",categoryList);
//		if(categoryList.size()>0){
//			map.putAll(searchBrandAndSpecList(categoryList.get(0)));
//		}
		//多条件查询
		if(!"".equals(searchMap.get("category"))){
			map.putAll(searchBrandAndSpecList(searchMap.get("category")));
		}else{
			if(categoryList.size()>0 && categoryList!=null){
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}
		return map;
	}
	private Map searchBrandAndSpecList(Object category){
		Map map=new HashMap();
		Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);//获取模板ID
		if(typeId!=null){
			//根据模板ID查询品牌列表 
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);//返回值添加品牌列表
			//根据模板ID查询规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);				
		}			
		return map;
	}

	private  List searchCategoryList(Map searchMap){
		//建立集合
		List<String> list=new ArrayList();	
		Query query=new SimpleQuery();		
		//按照关键字查询
		Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions=new GroupOptions().addGroupByField("item_category");
		query.setGroupOptions(groupOptions);
		//得到分组页
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		//根据列得到分组结果集
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//得到分组结果入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		//得到分组入口集合
		List<GroupEntry<TbItem>> content = groupEntries.getContent();
		for(GroupEntry<TbItem> entry:content){
			list.add(entry.getGroupValue());//将分组结果的名称封装到返回值中	
		}
		return list;
	}
	
	private Map searchList(Map searchMap){
		
		Map map=new HashMap();
		
		String keyword = (String) searchMap.get("keywords");
		
		HighlightQuery query = new SimpleHighlightQuery();
		
		HighlightOptions highlightOptions = new HighlightOptions();
		highlightOptions.addField("item_title");
		highlightOptions.addField("item_seller");
		highlightOptions.setSimplePrefix("<em style='color:red'>");
		highlightOptions.setSimplePostfix("</em>");
		query.setHighlightOptions(highlightOptions);
		
		
		if(keyword!=null && !keyword.equals("")){
			Criteria criteria = new Criteria("item_keywords").is(keyword);
			query.addCriteria(criteria);
		}
		
		//分类查询
		if(searchMap.get("category") !=null && !"".equals(searchMap.get("category"))){
			Criteria criteria = new Criteria("item_category").is(searchMap.get("category"));
			query.addCriteria(criteria);
		}
		//品牌查询
		if(searchMap.get("brand") !=null && !"".equals(searchMap.get("brand"))){
			Criteria criteria = new Criteria("item_brand").is(searchMap.get("brand"));
			query.addCriteria(criteria);
		}
		
		//规格查询
		if(searchMap.get("spec") !=null){
			Map<String,String> specMap= (Map) searchMap.get("spec");
			for(String key:specMap.keySet() ){
				Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);				
			}
		}
		//价格筛选
		if(!"".equals(searchMap.get("price"))){
			String[] price = ((String) searchMap.get("price")).split("-");
			if(!price[0].equals("0")){//如果区间起点不等于0
				Criteria filterCriteria=new Criteria("item_price").greaterThanEqual(price[0]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);				
			}
			if(!price[1].equals("*")){//如果区间终点不等于*
				Criteria filterCriteria=new  Criteria("item_price").lessThanEqual(price[1]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);				
			}
		}
		//分页查询
		Integer pageNum= (Integer) searchMap.get("pageNum");//提取页码
		if(pageNum==null){
			pageNum=1;//默认第一页
		}
		Integer pageSize=(Integer) searchMap.get("pageSize");//每页记录数 
		if(pageSize==null){
			pageSize=20;//默认20
		}
		query.setOffset((pageNum-1)*pageSize);//从第几条记录查询
		query.setRows(pageSize);
		//排序
		String sortValue= (String) searchMap.get("sort");//ASC  DESC  
		String sortField= (String) searchMap.get("sortField");//排序字段
		if(sortValue!=null && !sortValue.equals("")){
			Sort sort = null;
			if(sortValue.equals("ASC")){
				sort=new Sort(Sort.Direction.ASC, "item_"+sortField);
				query.addSort(sort);
			}
			if(sortValue.equals("DESC")){		
				sort=new Sort(Sort.Direction.DESC, "item_"+sortField);
				query.addSort(sort);
			}			
		}
		
		HighlightPage<TbItem> queryForHighlightPage = solrTemplate.queryForHighlightPage(query,TbItem.class);
		
		List<HighlightEntry<TbItem>> highlighted = queryForHighlightPage.getHighlighted();
		
		for (HighlightEntry<TbItem> highlightEntry : highlighted) {
			for (Highlight Entry : highlightEntry.getHighlights()) {
				if("item_title".equals(Entry.getField().getName())){
					highlightEntry.getEntity().setTitle(Entry.getSnipplets().get(0));
				}
				if("item_seller".equals(Entry.getField().getName())){
					highlightEntry.getEntity().setSeller(Entry.getSnipplets().get(0));
				}
			}
		}
		map.put("rows", queryForHighlightPage.getContent());
		map.put("totalPages", queryForHighlightPage.getTotalPages());
		map.put("total", queryForHighlightPage.getTotalElements());
		return map;
	}
	@Override
	public void importList(List list) {
		solrTemplate.saveBeans(list);	
		solrTemplate.commit();
	}
	@Override
	public void deleteByGoodsIds(String id) {
		Query query = new SimpleQuery("item_goodsid:"+id);
		solrTemplate.delete(query);
		solrTemplate.commit();
		System.out.println("删除索引库");
	}
	
}
