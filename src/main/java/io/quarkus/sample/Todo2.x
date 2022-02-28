package io.quarkus.sample;

import org.bson.types.ObjectId;
import java.util.Objects;

public class Todo {

    private ObjectId id;
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String title;
    public boolean completed;
    public int order;
    public String url;
    

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Todo)) {
            return false;
        }

        Todo other = (Todo) obj;

        return Objects.equals(other.title, this.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title);
    }

}
