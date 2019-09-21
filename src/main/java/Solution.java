import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Solution {

    enum CMD {
        STOP,
        OPEN,
        ENTER,
        EXIT,
        CLOSE,
        UP,
        DOWN
    }

    private final static int MAX = Integer.MAX_VALUE;

    // given
    public CloseableHttpClient client = HttpClientBuilder.create().build();

    public String url = "http://localhost:8000";
    public String user = "tester2";
    public int problem = 1;
    public int count = 1;

    public String token;
    public List<Elevator> elevators;
    public List<Call> calls;
    public boolean is_end;

    public List<Command> commands;

    public ResponseDTO res;

    // when
//    CloseableHttpResponse res = client.execute(new HttpGet("https://nghttp2.org/httpbin/get"));
//    String body = EntityUtils.toString(res.getEntity());
//    JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();


    public static void main(String[] args) throws IOException {
        Solution s = new Solution();

    }

    public Solution() throws IOException {
        startApi();
        while (true) {
            onCalls();

            if (res.is_end) break;

            action();
        }
    }

    public boolean action() throws IOException {
        commands = new ArrayList<>();

        for (Elevator e : res.elevators) {
            Call target = null;
            target = chooseTarget(e);
            if (target == null || e.isFull()) {
                // 더이상 태울애가 없을 때,  : target == null
                // 만원일 때,              : e.isFull() == true

                // 일단 열고 있으면 문 닫자
                if (e.status == STATUS.OPENED) {
                    // 내릴 사람이 있으면 내리고 태우자.
                    if (e.canExit()) {
                        // 내릴사람 있으면 내린다.
                        cmd(e, CMD.EXIT);
                    }else {
                        cmd(e, CMD.CLOSE);
                    }
                } else {
                    if (e.isEmpty()) {
                        // 태우고 있는 애가 없을 때는 멈추자.
                        cmd(e, CMD.STOP);
                    } else {
                        // 태우고 있는 애가 있을 때는 내려주러 가고,
                        // 데리고 있는 애 중에 내려줄 목적지를 정한다.
                        target = chooseTargetOfPassengers(e);

                        // target 위치에 따라서 이동한다.
                        if (e.floor < target.end) {
                            // 엘베가 target 보다 밑에 있을 때
                            cmd(e, CMD.UP);
                        } else if ( e.floor > target.end){
                            // 엘베가 target 보다 위에 있을 때
                            cmd(e, CMD.DOWN);
                        } else {
                            // 내려줘야지.
                            if (e.status == STATUS.STOPPED) {
                                cmd(e, CMD.OPEN);
                            } else if (e.status == STATUS.OPENED) {
                                // 내릴 사람이 있으면 내리고 태우자.
                                if (e.canExit()) {
                                    // 내릴사람 있으면 내린다.
                                    cmd(e, CMD.EXIT);
                                }
                            } else{
                                cmd(e, CMD.STOP);
                            }
                        }
                    }
                }
            } else {
                // target이 있다는 뜻은,
                // ! e.isFull()
                if (e.floor == target.start) {
                    // target과 층이 같으면 태운다.
                    if (e.status == STATUS.STOPPED) {
                        cmd(e, CMD.OPEN);
                    } else if (e.status == STATUS.OPENED) {
                        // 내릴 사람이 있으면 내리고 태우자.
                        if (e.canExit()) {
                            // 내릴사람 있으면 내린다.
                            cmd(e, CMD.EXIT);
                        } else {
                            // 태울 사람 있으면 태운다.
                            // TODO 누굴 태울지에 대한 로직은 만들 수가 없구나. 젠장
                            // TODO 방향 같은 놈만 태우고 싶은데, 이건 target 잡을 때 해야할 듯
                            cmd(e, CMD.ENTER);
                        }
                    } else{
                        cmd(e, CMD.STOP);
                    }
                } else {
                    // target이 층이 다르면 타겟으로 이동하자.
                    if (e.floor < target.start) {
                        // 엘베가 target 보다 밑에 있을 때
                        if(e.status == STATUS.STOPPED || e.status == STATUS.UPWARD) {
                            cmd(e, CMD.UP);
                        }else if( e.status == STATUS.OPENED ) {
                            cmd(e, CMD.CLOSE);
                        }else{
                            cmd(e, CMD.STOP);
                        }
                    } else {
                        // 엘베가 target 보다 위에 있을 때
                        if(e.status == STATUS.STOPPED || e.status == STATUS.DOWNWARD) {
                            cmd(e, CMD.DOWN);
                        }else if( e.status == STATUS.OPENED ) {
                            cmd(e, CMD.CLOSE);
                        }else{
                            cmd(e, CMD.STOP);
                        }
                    }

                }
            }
        }

        HttpPost req = new HttpPost(url + "/action");
        req.addHeader("X-Auth-Token", res.token);
        req.setHeader("Accept", "application/json");
        req.setHeader("Content-type", "application/json");

        Map<String, List> param = new HashMap<>();
        param.put("commands", commands);
        String jsonParam = new Gson().toJson(param);
        StringEntity input = new StringEntity(jsonParam, Consts.UTF_8);
        req.setEntity(input);

        CloseableHttpResponse closeableHttpResponse = client.execute(req);

        if (closeableHttpResponse.getStatusLine().getStatusCode() == 200) {
            String body = EntityUtils.toString(closeableHttpResponse.getEntity());
            JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
            res = new Gson().fromJson(jsonObject, ResponseDTO.class);

            elevators = res.elevators;

            // action finished;
            return true;
        }else{
            while(closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                for(Command c : commands){
                    for(Elevator e : elevators){
                        if( e.id == c.elevator_id){
                            e.max = e.passengers.size()+c.call_ids.size()-1;
                        }
                    }
                    c.call_ids.remove(0);
                }
                closeableHttpResponse = client.execute(req);
            }
            String body = EntityUtils.toString(closeableHttpResponse.getEntity());
            JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
            res = new Gson().fromJson(jsonObject, ResponseDTO.class);

//            for (Call c : res.calls) {
//                c.setDirection();
//            }
            elevators = res.elevators;

            // action finished. when over max;
            return true;
        }
//        return false;
    }


    private boolean cmd(Elevator e, CMD cmd) {
        Command command = new Command();

        command.elevator_id = e.id;
        command.command = cmd.name();

        if (cmd == CMD.EXIT) {
            command.call_ids = new ArrayList<>();
            // 이 층에 내릴 애들 다 내리게 하자.
            // 그냥 다 내리게 해도 될듯.
            // 어차피 태울 거니까.
            for(Call c : e.passengers){
                if( c.end == e.floor) {
                    command.call_ids.add(c.id);
                }
            }
        }else if( cmd == CMD.ENTER){
            command.call_ids = new ArrayList<>();

            List<Call> sameFloorCalls =
                    res.calls.stream().filter(t->t.start == e.floor).collect(Collectors.toList());

            int cntSameFloor = sameFloorCalls.size();
            int cntPassengers = e.passengers.size();
            if( e.max >= cntPassengers + cntSameFloor){
                for(Call c : sameFloorCalls){
                    command.call_ids.add(c.id);
                }
            }else{
                // 어떤 애들 먼저 태울까.

                int cntUpward = 0;
                int cntDownward = 0;
                for(Call c : e.passengers){
                    if(c.end > e.floor){
                        cntUpward++;
                    }else if(c.end < e.floor){
                        cntDownward++;
                    }
                }

                // TODO 누굴 먼저 태울지는 나중에 고안
//                List<Call> sortedList = sameFloorCalls.stream().sorted(new Comparator<Call>() {
//                    @Override
//                    public int compare(Call o1, Call o2) {
//                        int distanceO1 = o1.
//                        return 0;
//                    }
//                })

                int cntWillRide = 0;
                for(Call c : sameFloorCalls){
                    command.call_ids.add(c.id);
                    cntWillRide++;
                    if( e.max <= cntPassengers + cntWillRide) {
                        break;
                    }
                }
            }
        }

        for (Command c : commands) {
            if (c.elevator_id == e.id) {
                return false;
            }
        }
        commands.add(command);

        // set command finished;
        return true;
    }


    private Call chooseTargetOfPassengers(Elevator e) {
        int minDistance = MAX;
        Call target = null;

        for (Call c : e.passengers) {
            int distance = Math.abs(e.floor - c.end);
            if (minDistance > distance) {
                minDistance = distance;
                target = c;
            }
        }

        // choose target in passengers
        return target;
    }

    private Call chooseTarget(Elevator e) {
        int minDistance = MAX;
        Call target = null;

        for (Call c : res.calls) {
//            int distance = Math.abs(e.floor - c.start);
            boolean sameDirection = true;
            int distance = c.start - e.floor;
            if (distance > 0) {
//                c.start > e.floor
                // 위로 가야하는데, 내가 방향이 내려가고 있었으면
                // 멈췄다가 다시 올라가야 하므로,
                if (e.status == STATUS.DOWNWARD) {
                    distance += 1;
                    sameDirection = false;
                }
            } else if (distance < 0) {
//                c.start < e.floor
                // 아래로 가야하는데, 내가 방향이 올라가고 있었으면
                // 멈췄다가 다시 내려가야 하므로,

                if (e.status == STATUS.UPWARD) {
                    distance -= 1;
                    sameDirection = false;
                }
                distance = Math.abs(distance);

            } else {
                // c.start == e.floor
                // 멈춘다.
            }

            if (minDistance > distance) {
                minDistance = distance;
                target = c;
            } else if (minDistance == distance) {
                if (sameDirection) {
                    target = c;
                }
            }
        }
        // choose target finished
        return target;
    }

    public boolean onCalls() throws IOException {
        HttpGet req = new HttpGet(url + "/oncalls");
        req.addHeader("X-Auth-Token", res.token);
        CloseableHttpResponse closeableHttpResponse = client.execute(req);

        if (closeableHttpResponse.getStatusLine().getStatusCode() == 200) {
            String body = EntityUtils.toString(closeableHttpResponse.getEntity());
            JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
            res = new Gson().fromJson(jsonObject, ResponseDTO.class);

//            for (Call c : res.calls) {
//                c.setDirection();
//            }

            this.elevators = res.elevators;
//            this.calls = res.calls;

            // oncall finished
            return true;
        }
        return false;
    }

    public boolean startApi() throws IOException {
        HttpPost req = new HttpPost(url + "/start" + "/" + user + "/" + problem + "/" + count);
//        req.addHeader("X-Auth-Token", res.token);
        CloseableHttpResponse closeableHttpResponse = client.execute(req);

        if (closeableHttpResponse.getStatusLine().getStatusCode() == 200) {
            String body = EntityUtils.toString(closeableHttpResponse.getEntity());
            JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();
            res = new Gson().fromJson(jsonObject, ResponseDTO.class);

            token = res.token;
            return true;
        }
        return false;
    }
}
