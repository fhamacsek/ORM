package model;
import java.sql.Timestamp;
import java.util.Set;

public class Orders {
    int id;
    Timestamp created_at;
    Clients client;

    Set<OrderLines> orderLines;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Clients getClient() {
        return client;
    }

    public void setClient(Clients client) {
        this.client = client;
    }

    public Set<OrderLines> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(Set<OrderLines> ol) {
        this.orderLines = ol;
    }
}