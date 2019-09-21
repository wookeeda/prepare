import java.util.List;

public class Elevator {
    int id;
    int floor;
    List<Call> passengers;
    STATUS status;
    int max = Integer.MAX_VALUE;

    public boolean isFull() {
        if(max <= passengers.size()){
            return true;
        }
        return false;
    }

    public boolean canExit() {
        for(Call c : passengers){
            if(c.end == floor){
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        if(passengers == null || passengers.size() == 0){
            return true;
        }
        return false;
    }
}
