package com.aisile.search.service.impl;

import java.util.List;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aisile.pojo.TbItem;
import com.aisile.search.service.ItemSearchService;
import com.alibaba.fastjson.JSON;

@Component
public class ItemSearchListener implements MessageListener{
	
	@Autowired
	private ItemSearchService itemSearchService;
	
	@Override
	public void onMessage(Message message) {
		System.out.println("监听接收到消息...");
		try {
			TextMessage textMessage = (TextMessage)message;
			String text = textMessage.getText();
			List<TbItem> list = JSON.parseArray(text, TbItem.class);
			for (TbItem tbItem : list) {
				Map map = JSON.parseObject(tbItem.getSpec());
				tbItem.setSpecMap(map);
			}
			itemSearchService.importList(list);//导入	
			System.out.println("成功导入到索引库");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
