package com.aisile.search.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aisile.search.service.ItemSearchService;
import com.aisile.solrutil.SolrUtil;
import com.alibaba.dubbo.config.annotation.Reference;

@RestController
@RequestMapping("/itemsearch")
public class ItemSearchController {
	
	@Reference
	private ItemSearchService itemSearchService;

	@RequestMapping("/search")
	public Map<String, Object> search(@RequestBody Map searchMap ){
		return  itemSearchService.search(searchMap);
	}
	
	@RequestMapping("/solr")
	public void solr() {
		SolrUtil solrUtil = new SolrUtil();
		solrUtil.main(null);
		System.out.println("456");
		solrUtil.importItemData();
	}
}
