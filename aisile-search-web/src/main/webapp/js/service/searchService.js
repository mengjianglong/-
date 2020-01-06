app.service("searchService",function($http){
	//根据分类ID查询广告列表
	this.search=function(searchMap){
		return $http.post('../itemsearch/search.do',searchMap);
	}
	this.solr=function(){
		return $http.get('../itemsearch/solr.do');
	}
});