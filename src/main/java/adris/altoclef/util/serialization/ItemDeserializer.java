package adris.altoclef.util.serialization;

import adris.altoclef.util.ItemUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemDeserializer extends StdDeserializer<Object> {
    public ItemDeserializer() {
        this(null);
    }

    public ItemDeserializer(Class<Object> vc) {
        super(vc);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        List<Item> result = new ArrayList<>();

        if (p.getCurrentToken() != JsonToken.START_ARRAY) {
            throw new JsonParseException("Start array expected", p.getCurrentLocation());
        }
        while (p.nextToken() != JsonToken.END_ARRAY) {
            Item item;
            if (p.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                // Old raw id (ew stinky)
                int rawId = p.getIntValue();
                item = Item.byRawId(rawId);
            } else {
                // Translation key (the proper way)
                String itemKey = p.getText();
                itemKey = ItemUtil.trimItemName(itemKey);
                item = Registry.ITEM.get(new Identifier(itemKey));
            }
            result.add(item);
        }

        return result;
    }
}
