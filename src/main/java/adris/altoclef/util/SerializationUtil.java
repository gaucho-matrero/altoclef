package adris.altoclef.util;

import adris.altoclef.util.csharpisbetter.Util;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SerializationUtil {
    class ItemSerializer extends StdSerializer<Object> {
        public ItemSerializer() {
            this(null);
        }

        public ItemSerializer(Class<Object> vc) {
            super(vc);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            List<Item> items = (List<Item>) value;
            gen.writeStartArray();
            for (Item item : items) {
                String key = ItemUtil.trimItemName(item.getTranslationKey());
                gen.writeString(key);
            }
            gen.writeEndArray();
        }
    }

    class ItemDeserializer extends StdDeserializer<Object> {
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

    class BlockPosSerializer extends StdSerializer<BlockPos> {
        public BlockPosSerializer() {
            this(null);
        }

        public BlockPosSerializer(Class<BlockPos> vc) {
            super(vc);
        }

        @Override
        public void serialize(BlockPos value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.getX() + ", " + value.getY() + ", " + value.getZ());
        }
    }

    class BlockPosDeserializer extends StdDeserializer<BlockPos> {
        public BlockPosDeserializer() {
            this(null);
        }

        public BlockPosDeserializer(Class<BlockPos> vc) {
            super(vc);
        }

        int trySet(JsonParser p, Map<String, Integer> map, String key) throws JsonParseException {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            throw new JsonParseException(p, "Blockpos should have key for " + key + " key, but one was not found.");
        }

        int tryParse(JsonParser p, String whole, String part) throws JsonParseException {
            try {
                return Integer.parseInt(part.trim());
            } catch (NumberFormatException e) {
                throw new JsonParseException(p, "Failed to parse blockpos string \""
                        + whole + "\", specificaly part \"" + part + "\".");
            }
        }

        @Override
        public BlockPos deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                String bposString = p.getValueAsString();
                String[] parts = bposString.split(",");
                if (parts.length != 3) {
                    throw new JsonParseException(p, "Invalid blockpos string: \"" + bposString + "\", must be in form \"x, y, z\".");
                }
                int x = tryParse(p, bposString, parts[0]);
                int y = tryParse(p, bposString, parts[1]);
                int z = tryParse(p, bposString, parts[2]);
                return new BlockPos(x, y, z);
            } else if (p.getCurrentToken() == JsonToken.START_OBJECT) {
                Map<String, Integer> parts = new HashMap<>();
                p.nextToken();
                while (p.getCurrentToken() != JsonToken.END_OBJECT) {
                    if (p.getCurrentToken() == JsonToken.FIELD_NAME) {
                        String fName = p.getCurrentName();
                        p.nextToken();
                        if (p.getCurrentToken() != JsonToken.VALUE_NUMBER_INT) {
                            throw new JsonParseException(p, "Expecting integer token for blockpos. Got: " + p.getCurrentToken());
                        }
                        parts.put(p.getCurrentName(), p.getIntValue());
                        p.nextToken();
                    } else {
                        throw new JsonParseException(p, "Invalid structure, expected field name (like x, y or z)");
                    }
                }
                if (parts.size() != 3) {
                    throw new JsonParseException(p, "Expected [x, y, z] keys to be part of a blockpos object. Got " + Util.arrayToString(Util.toArray(String.class, parts.keySet())));
                }
                int x = trySet(p, parts, "x");
                int y = trySet(p, parts, "y");
                int z = trySet(p, parts, "z");
                return new BlockPos(x, y, z);
            }
            throw new JsonParseException(p, "Invalid token: " + p.getCurrentToken());
        }
    }

}
