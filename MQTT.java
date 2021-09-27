public class MQTT implements MqttCallback {
    private final String TAG = "TAG_MQTT";

    private MqttClient client;
    private MqttConnectOptions option;

    private Consumer<HashMap<Object, Object>> messageArrivedCallback = null;
    private Consumer<HashMap<Object, Object>> connectionLostCallback = null;
    private Consumer<HashMap<Object, Object>> deliveryCompleteCallback = null;

    // MQTT 연결 객체 생성
    public MQTT(String userName, String password, String serverUri) {
        //이미 연결 중이라면, 연결 세션 클리어
        if (client != null && client.isConnected())
            clear();

        String clientId = UUID.randomUUID().toString();
        option = new MqttConnectOptions();
        option.setKeepAliveInterval(30);
        option.setUserName(userName);
        option.setPassword(password.toCharArray());
        try {
            client = new MqttClient(serverUri, clientId, new MemoryPersistence());
            client.setCallback(this);
            client.connect(option);
        } catch (Exception e) {
            Log.e(TAG, "연결 오류 : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // MQTT 메세지 전송
    public boolean send(String topic, String payload) {
        MqttMessage msg = new MqttMessage();
        msg.setPayload(payload.getBytes(StandardCharsets.UTF_8));
        try {
            client.publish(topic, msg);
            return true;
        } catch (MqttException e) {
            Log.e(TAG, "전송 오류 : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // topic 구독
    public boolean subscribe(String... topics) {
        try {
            if (topics != null) {
                for (String topic : topics)
                    client.subscribe(topic, 0);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "구독 오류 : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 기존 MQTT의 연결이 끊어진 상태라면 재연결
    public void reconnect() {
        if (client != null && client.isConnected() == false) {
            try {
                client.reconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    // MQTT 연결 세션 초기화
    public void clear() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception e) {
                Log.e(TAG, "닫기 오류 : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 메세지 도착 이후 실행 될 기능을 정의
    public void addMessageArriveCallback(Consumer<HashMap<Object, Object>> onMessageArrived) {
        messageArrivedCallback = onMessageArrived;
    }

    // MQTT 연결이 끊긴 이후 실행 될 기능을 정의
    public void addConnectionLostCallback(Consumer<HashMap<Object, Object>> onConnectionLost) {
        connectionLostCallback = onConnectionLost;
    }

    // 메세지 전달이 완료 된 이후 실행 될 기능을 정의
    public void addDeliveryCompleteCallback(Consumer<HashMap<Object, Object>> onDeliveryComplete) {
        deliveryCompleteCallback = onDeliveryComplete;
    }

    //연결이 끊겼을 때 호출 됨
    @Override
    public void connectionLost(Throwable throwable) {
        throwable.printStackTrace();
        if (connectionLostCallback != null) {
            HashMap<Object, Object> result = new HashMap<>();
            result.put("result", throwable);
            try {
                connectionLostCallback.accept(result);
            } catch (Throwable error) {
                Log.e(TAG, "연결 유실 오류 : " + error.getMessage());
                error.printStackTrace();
            }
        }
    }

    //메세지가 도착했을 때 호출 됨
    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        Log.d(TAG, "메세지 도착함 : " + payload);
        try {
            if (messageArrivedCallback == null) {
                throw new Exception("메세지 응답에 대한 콜백이 정의 되어있지 않습니다. " +
                        "addMessageArriveCallback()를 호출하여 메세지 처리 콜백을 등록해주세요.");
            }
            HashMap<Object, Object> result = new HashMap<>();
            result.put("topic", topic);
            result.put("message", payload);
            messageArrivedCallback.accept(result);
        } catch (Throwable throwable) {
            Log.e(TAG, "메세지 수신 오류 : " + throwable.getMessage());
            Log.d(TAG, "받은 메세지 정보 : topic=" + topic + ", payload=" + payload);
            throwable.printStackTrace();
        }
    }

    //전송이 완료 된 이후 호출 됨
    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        if (deliveryCompleteCallback != null) {
            HashMap<Object, Object> result = new HashMap<>();
            result.put("result", iMqttDeliveryToken);
            try {
                deliveryCompleteCallback.accept(result);
            } catch (Throwable error) {
                Log.e(TAG, "연결 유실 오류 : " + error.getMessage());
                error.printStackTrace();
            }
        }
    }

    //MQTT의 현재 연결상태 확인
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
