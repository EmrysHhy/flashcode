package com.bitejiuyeke.bitemstemplateservice.rabbit;

import com.bitejiuyeke.bitemstemplateservice.domain.MessageDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void produceMsg(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend("testQueue", messageDTO);
    }
}