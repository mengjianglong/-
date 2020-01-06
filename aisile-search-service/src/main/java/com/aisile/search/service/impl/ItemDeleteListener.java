package com.aisile.search.service.impl;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aisile.search.service.ItemSearchService;

@Component
public class ItemDeleteListener implements MessageListener{

	@Autowired
	private ItemSearchService itemSearchService;

	@Override
	public void onMessage(Message message) {
		System.out.println("**********");
		TextMessage textMessage = (TextMessage)message;
		try {
			System.out.println(textMessage.getText());
			itemSearchService.deleteByGoodsIds(textMessage.getText());
			System.out.println("删除成功");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("删除失败");
		}			
	}
}
