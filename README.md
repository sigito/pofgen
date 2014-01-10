pofgen
======

Primitive [PofSerializer](http://download.oracle.com/otn_hosted_doc/coherence/340/com/tangosol/io/pof/PofSerializer.html) generator plugin for IntelliJ IDEA.

Adds new generate menu item 'Generate PofSerializer' in class context. After selection of fields to use for serialization and deserialization, creates [PofSerializer](http://download.oracle.com/otn_hosted_doc/coherence/340/com/tangosol/io/pof/PofSerializer.html).

Example
-------

Suppose we have such Entity declaration
```java
public class Entity {
    private final long id;
    private String description;
    private Object value;

    public Entity(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
```
as the result of generation we'd get companion serializer.
```java
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

public class EntityPofSerializer implements PofSerializer {
    private static final int ID = 0;
    private static final int DESCRIPTION = 1;
    private static final int VALUE = 2;

    public void serialize(PofWriter pofWriter, Object o) throws IOException {
        Entity entity = (Entity) o;
        pofWriter.writeLong(ID, entity.getId());
        pofWriter.writeString(DESCRIPTION, entity.getDescription());
        pofWriter.writeObject(VALUE, entity.getValue());
        pofWriter.writeRemainder(null);
    }

    public Entity deserialize(PofReader pofReader) throws IOException {
        long id = pofReader.readLong(ID);
        String description = pofReader.readString(DESCRIPTION);
        Object value = pofReader.readObject(VALUE);
        Entity entity = new Entity(id);
        entity.setDescription(description);
        entity.setValue(value);
        pofReader.readRemainder();
        return entity;
    }
}
```
  
