package com.voicecall.demo.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.voicecall.demo.bean.EventData;
import com.voicecall.demo.bean.RoomInfo;
import com.voicecall.demo.bean.UserBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.voicecall.demo.config.MemCons.rooms;

/**
 * @Autor: cxj
 * @Date: 2023-01-30
 * @Description:
 * @Version: V1.0
 */
@Slf4j
@ServerEndpoint("/ws/{userId}")
@Component
public class WebSocketServer {
    private static String avatar = "p1.jpeg";
    private String userId;
    private static Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean == null) {
            userBean = new UserBean(userId, avatar);
        }
        userBean.setPcSession(session, 1);
        log.info("PC用户登陆:" + userBean.getUserId() + ",session:" + session.getId());
        this.userId = userId;
        //加入列表
        MemCons.userBeans.put(userId, userBean);
        ConcurrentHashMap<String, UserBean> userBeans = MemCons.userBeans;
        Collection<UserBean> values = userBeans.values();
        // 登陆成功，返回个人信息
        EventData send = new EventData();
        send.setEventName("__login_success");
        Map<String, Object> map = new HashMap<>();
        map.put("userID", userId);
        map.put("avatar", avatar);
        send.setData(map);
        session.getAsyncRemote().sendText(gson.toJson(send));
    }

    // 用户下线
    @OnClose
    public void onClose() {
        System.out.println(userId + "-->onClose......");
        // 根据用户名查出房间,
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean != null) {
            Session pcSession = userBean.getPcSession();
            if (pcSession != null) {
                try {
                    pcSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                userBean.setPcSession(null, 0);
                MemCons.userBeans.remove(userId);
                log.info("PC用户离开:" + userBean.getUserId());
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("receive data:" + message);
        handleMessage(message, session);
    }

    private void handleMessage(String message, Session session) {
        EventData data;
        try {
            data = gson.fromJson(message, EventData.class);
        } catch (JsonSyntaxException e) {
            System.out.println("json解析错误：" + message);
            return;
        }
        switch (data.getEventName()) {
            case "__create":
                createRoom(message, data.getData());
                break;
            case "__invite":
                invite(message, data.getData());
                break;
            case "__ring":
                ring(message, data.getData());
                break;
            case "__cancel":
                cancel(message, data.getData());
                break;
            case "__reject":
                reject(message, data.getData());
                break;
            case "__join":
                join(message, data.getData());
                break;
            case "__ice_candidate":
                iceCandidate(message, data.getData());
                break;
            case "__offer":
                offer(message, data.getData());
                break;
            case "__answer":
                answer(message, data.getData());
                break;
            case "__leave":
                leave(message, data.getData());
                break;
            case "__audio":
                transAudio(message, data.getData());
                break;
            case "__disconnect":
                disconnet(message, data.getData());
                break;
            case "__preinvite" :
                preinvite(message,data.getData());
                break;
            default:
                break;
        }

    }

    // 创建房间
    private void createRoom(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String userId = (String) data.get("userID");

        System.out.println(String.format("createRoom:%s ", room));

        RoomInfo roomParam = rooms.get(room);
        // 没有这个房间
        if (roomParam == null) {
            int size = (int) Double.parseDouble(String.valueOf(data.get("roomSize")));
            // 创建房间
            RoomInfo roomInfo = new RoomInfo();
            roomInfo.setMaxSize(size);
            roomInfo.setRoomId(room);
            roomInfo.setUserId(userId);
            // 将房间储存起来
            rooms.put(room, roomInfo);
            CopyOnWriteArrayList<UserBean> copy = new CopyOnWriteArrayList<>();
            // 将自己加入到房间里
            UserBean my = MemCons.userBeans.get(userId);
            copy.add(my);
            rooms.get(room).setUserBeans(copy);
            EventData send = new EventData();
            send.setEventName("__peers");
            Map<String, Object> map = new HashMap<>();
            map.put("connections", "");
            map.put("you", userId);
            map.put("roomSize", size);
            send.setData(map);
            System.out.println(gson.toJson(send));
            sendMsg(my, -1, gson.toJson(send));

        }

    }

    // 首次邀请
    private void invite(String message, Map<String, Object> data) {
        String userList = (String) data.get("userList");
        String room = (String) data.get("room");
        String inviteId = (String) data.get("inviteID");
        boolean audioOnly = false;
        String[] users = userList.split(",");

        System.out.println(String.format("room:%s,%s invite %s audioOnly:%b", room, inviteId, userList, audioOnly));
        // 给其他人发送邀请
        for (String user : users) {
            UserBean userBean = MemCons.userBeans.get(user);
            if (userBean != null) {
                sendMsg(userBean, -1, message);
            }
        }


    }

    // 响铃回复
    private void ring(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String inviteId = (String) data.get("toID");

        UserBean userBean = MemCons.userBeans.get(inviteId);
        if (userBean != null) {
            sendMsg(userBean, -1, message);
        }
    }

    // 取消拨出
    private void cancel(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String userList = (String) data.get("userList");
        String[] users = userList.split(",");
        for (String userId : users) {
            UserBean userBean = MemCons.userBeans.get(userId);
            if (userBean != null) {
                sendMsg(userBean, -1, message);
            }
        }

        if (MemCons.rooms.get(room) != null) {
            MemCons.rooms.remove(room);
        }


    }

    // 拒绝接听
    private void reject(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String toID = (String) data.get("toID");
        UserBean userBean = MemCons.userBeans.get(toID);
        if (userBean != null) {
            sendMsg(userBean, -1, message);
        }
        RoomInfo roomInfo = MemCons.rooms.get(room);
        if (roomInfo != null) {
            if (roomInfo.getMaxSize() == 2) {
                MemCons.rooms.remove(room);
            }
        }


    }

    // 加入房间
    private void join(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String userID = (String) data.get("userID");

        RoomInfo roomInfo = rooms.get(room);

        int maxSize = roomInfo.getMaxSize();
        CopyOnWriteArrayList<UserBean> roomUserBeans = roomInfo.getUserBeans();

        //房间已经满了
        if (roomUserBeans.size() >= maxSize) {
            return;
        }
        UserBean my = MemCons.userBeans.get(userID);
        // 1. 將我加入到房间
        roomUserBeans.add(my);
        roomInfo.setUserBeans(roomUserBeans);
        rooms.put(room, roomInfo);

        // 2. 返回房间里的所有人信息
        EventData send = new EventData();
        send.setEventName("__peers");
        Map<String, Object> map = new HashMap<>();

        String[] cons = new String[roomUserBeans.size()];
        for (int i = 0; i < roomUserBeans.size(); i++) {
            UserBean userBean = roomUserBeans.get(i);
            if (userBean.getUserId().equals(userID)) {
                continue;
            }
            cons[i] = userBean.getUserId();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cons.length; i++) {
            if (cons[i] == null) {
                continue;
            }
            sb.append(cons[i]).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        map.put("connections", sb.toString());
        map.put("you", userID);
        map.put("roomSize", roomInfo.getMaxSize());
        send.setData(map);
        sendMsg(my, -1, gson.toJson(send));


        EventData newPeer = new EventData();
        newPeer.setEventName("__new_peer");
        Map<String, Object> sendMap = new HashMap<>();
        sendMap.put("userID", userID);
        newPeer.setData(sendMap);

        // 3. 给房间里的其他人发送消息
        for (UserBean userBean : roomUserBeans) {
            if (userBean.getUserId().equals(userID)) {
                continue;
            }
            sendMsg(userBean, -1, gson.toJson(newPeer));
        }


    }

    // 切换到语音接听
    private void transAudio(String message, Map<String, Object> data) {
        String userId = (String) data.get("userID");
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean == null) {
            System.out.println("用户 " + userId + " 不存在");
            return;
        }
        sendMsg(userBean, -1, message);
    }

    // 意外断开
    private void disconnet(String message, Map<String, Object> data) {
        String userId = (String) data.get("userID");
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean == null) {
            System.out.println("用户 " + userId + " 不存在");
            return;
        }
        sendMsg(userBean, -1, message);
    }

    // 发送offer
    private void offer(String message, Map<String, Object> data) {
        System.out.println(data);
        String userId = (String) data.get("userID");
        UserBean userBean = MemCons.userBeans.get(userId);
        sendMsg(userBean, -1, message);
    }

    // 发送answer
    private void answer(String message, Map<String, Object> data) {
        String userId = (String) data.get("userID");
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean == null) {
            System.out.println("用户 " + userId + " 不存在");
            return;
        }
        sendMsg(userBean, -1, message);

    }

    // 发送ice信息
    private void iceCandidate(String message, Map<String, Object> data) {
        String userId = (String) data.get("userID");
        UserBean userBean = MemCons.userBeans.get(userId);
        if (userBean == null) {
            System.out.println("用户 " + userId + " 不存在");
            return;
        }
        sendMsg(userBean, -1, message);
    }

    private void sendData(UserBean userBean, Object data) {
        Session pcSession = userBean.getPcSession();
        if (pcSession != null) {
            synchronized (object) {
                System.out.println(gson.toJson(data));
                pcSession.getAsyncRemote().sendText(gson.toJson(data));
            }
        }
    }

    private void preinvite(String message, Map<String,Object> data){
        String myid = (String) data.get("userid");
        String toid = (String) data.get("toid");
        UserBean userBean = MemCons.userBeans.get(toid);
        if(userBean == null){
            System.out.println("用戶" + userId + "不存在");
            return;
        }
        sendMsg(userBean, -1 ,message);

    }

    // 离开房间
    private void leave(String message, Map<String, Object> data) {
        String room = (String) data.get("room");
        String userId = (String) data.get("fromID");

        if (userId == null) return;
        RoomInfo roomInfo = MemCons.rooms.get(room);
        CopyOnWriteArrayList<UserBean> roomInfoUserBeans = roomInfo.getUserBeans();
        Iterator<UserBean> iterator = roomInfoUserBeans.iterator();
        MemCons.rooms.remove(room);
        while (iterator.hasNext()) {
            UserBean userBean = iterator.next();
            if (userId.equals(userBean.getUserId())) {
                continue;
            }
            sendMsg(userBean, -1, message);

            if (roomInfoUserBeans.size() == 1) {
                System.out.println("房间里只剩下一个人");
                if (roomInfo.getMaxSize() == 2) {
                    MemCons.rooms.remove(room);
                }
            }

            if (roomInfoUserBeans.size() == 0) {
                System.out.println("房间无人");
                MemCons.rooms.remove(room);
            }
        }


    }


    private static final Object object = new Object();

    // 给不同设备发送消息
    private void sendMsg(UserBean userBean, int device, String str) {
        System.out.println("发送给：" + userBean.getUserId());
        System.out.println("数据是：");
        System.out.println(str);
        Session pcSession = userBean.getPcSession();
        if (pcSession != null) {
            synchronized (object) {
                pcSession.getAsyncRemote().sendText(str);
            }
        }
        System.out.println("发送完毕");
    }
}