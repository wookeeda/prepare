import java.util.List;

public class ResponseDTO {
    String token;
    int timestamp;
    List<Elevator> elevators;
    List<Call> calls;
    boolean is_end;
}
