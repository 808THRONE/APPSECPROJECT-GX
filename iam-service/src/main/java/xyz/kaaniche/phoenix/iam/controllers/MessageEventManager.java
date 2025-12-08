package xyz.kaaniche.phoenix.iam.controllers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import xyz.kaaniche.phoenix.iam.boundaries.PushWebSocketEndpoint;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Singleton
@LocalBean
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MessageEventManager {

    @Inject @MessageEvent(MessageEvent.Type.CLIENT)
    private Event<JsonObject> clientMessage;

    @Inject @MessageEvent(MessageEvent.Type.MQTT)
    private Event<JsonObject> mqttMessage;

    @Resource
    private SessionContext sessionContext;

    public void publishFromClient(JsonObject message){
        clientMessage.fire(message);
    }

    public void publishFromMQTTBroker(JsonObject message){
        mqttMessage.fire(message);
    }

    private final Config config = ConfigProvider.getConfig();

    private final String mqttOverWebSocketURIString = config.getValue("mqtt.over.ws.uri.string",String.class);
    private final String mqttClientId = config.getValue("mqtt.client.id",String.class);
    private final String mqttUsername = config.getValue("mqtt.broker.username",String.class);
    private final String mqttPassword = config.getValue("mqtt.broker.password",String.class);
    private IMqttAsyncClient client;

    @PostConstruct
    public void start(){
        try {
            client = new MqttAsyncClient(mqttOverWebSocketURIString,mqttClientId,new MemoryPersistence());
            final MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.getBytes(StandardCharsets.UTF_8));
            client.connect(options);
            client.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

                }

                @Override
                public void mqttErrorOccurred(MqttException e) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    JsonObject jsonObject = Json.createObjectBuilder()
                            .add("topic",topic)
                            .add("payload", Json.createReader(new StringReader(new String(mqttMessage.getPayload()))).readObject().toString())
                            .add("id",mqttMessage.getId())
                            .build();
                    publishFromMQTTBroker(jsonObject);
                }

                @Override
                public void deliveryComplete(IMqttToken iMqttToken) {

                }

                @Override
                public void connectComplete(boolean b, String s) {

                }

                @Override
                public void authPacketArrived(int i, MqttProperties mqttProperties) {

                }
            });
        } catch (MqttException e) {
            throw new EJBException(e);
        }
    }

    public void consumeClientMessage(@Observes @MessageEvent(MessageEvent.Type.CLIENT)JsonObject clientMessage){
        boolean toClients = clientMessage.getBoolean("toClients");
        boolean toMqttBroker = clientMessage.getBoolean("toMQTTBroker");
        if(toClients) {
            PushWebSocketEndpoint.broadcastMessage(clientMessage);
        }
        if(toMqttBroker){
            int qos = clientMessage.getInt("qos");
            boolean retained = clientMessage.getBoolean("retained");
            byte[] payload = clientMessage.getJsonObject("payload").toString().getBytes(StandardCharsets.UTF_8);
            try {
                client.publish(clientMessage.getString("topic"),new MqttMessage(payload,qos,retained,null));
            } catch (MqttException e) {
                //Generate Rollback Message
                this.clientMessage.fire(Json.createObjectBuilder().add("mqttException",e.getMessage()).build());
                sessionContext.setRollbackOnly();
                throw new EJBException(e);
            }
        }
    }


    public void consumeMQTTMessage(@Observes @MessageEvent(MessageEvent.Type.MQTT)JsonObject mqttMessage){
        PushWebSocketEndpoint.broadcastMessage(mqttMessage);
        /*Alternatively:
        String topic =  mqttMessage.getString("topic");
        switch (topic){
            case "topic1":
                //persist something
                break;
            case "topic2":
                //persist some other stuff
                break;
            case "topic3":
                //doSomething
                break;
            default:
                PushWebSocketEndpoint.broadcastMessage(mqttMessage);
        }
        OR Call a Customized MessageDispatcher
        */
    }
}
