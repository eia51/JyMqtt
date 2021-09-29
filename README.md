# MqttExample
안드로이드에서 클라이언트, 서버, 임베디드 간 메세지를 주고 받고 싶을 때 사용 할 수 있는 MQTT 프로토콜의 간단한 사용 방법에 대해서 소개합니다.

## How to use
1. 본 레포지토리에 있는 `MQTT.java` 파일을 프로젝트 경로에 포함 (gradle로 분리 예정)

2. **연결 및 콜백 초기화**
   ```java     
     final String MQTT_URL_PUSH_HOST = "tcp://호스트이름:1883";
     final String MQTT_USER_NAME = "사용자이름";
     final String MQTT_PASSWORD = "비밀번호";
  
     // MQTT 연결
     MQTT mqtt = new MQTT(MQTT_USER_NAME, MQTT_PASSWORD, MQTT_URL_PUSH_HOST);

     // 메세지 수신 처리
     mqtt.addMessageArriveCallback(map -> {
         String topic = Objects.requireNonNull(map.get("topic")).toString();
         String message = Objects.requireNonNull(map.get("message")).toString();
         Log.d(TAG, String.format("Message received %s : %s", topic, message));
     });

     // 연결 오류 처리
     mqtt.addConnectionLostCallback(map -> {
         Object o = map.get("result");
         if (o instanceof Throwable)
             Log.e(TAG, ((Throwable) o).getMessage());
     });

     // 전달완료 콜백 처리
     mqtt.addDeliveryCompleteCallback(map -> {
         Object o = map.get("result");
         if (o instanceof IMqttDeliveryToken) {
             IMqttDeliveryToken token = (IMqttDeliveryToken) o;
             Log.d(TAG, "MQTT 전달완료 : " + token.getMessage());
         }
     });
   ```
   
<br/>
   
3. **구독 / 메세지 발행**
   ```java
      // 주제 구독
      mqtt.subscribe(topic);
      mqtt.subscribe(topic1, topic2, topic3);
      
      // 메세지 발행
      mqtt.send(보낼주제, 보낼 메세지);
   ```

<br/>

4. **기타 처리** 
   > - ***앱이 백그라운드로 숨었다가 다시 포그라운드로 돌아왔을 때 `onResume()`에서 `mqtt.reconnect()` 호출*** <br/>
   >   = 이전 커넥션 정보로 다시 MQTT 연결 시도
   >   
   > - ***onStop() onDestroy() 등 더 이상 앱이 사용되지 않을 때 `mqtt.clear()` 호출*** <br/>
   >   = Android 8.0 이상에서는 백그라운드 서비스를 제한하기 때문에 포그라운드 서비스를 사용

