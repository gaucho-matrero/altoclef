package adris.altoclef.util.serialization;

import com.fasterxml.jackson.core.JsonToken;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;

public class BlockPosDeserializer extends AbstractVectorDeserializer<BlockPos, Integer> {
    @Override
    protected String getTypeName() {
        return "BlockPos";
    }

    @Override
    protected String[] getComponents() {
        return new String[]{"x", "y", "z"};
    }

    @Override
    protected Integer parseUnit(String unit) throws Exception {
        return Integer.parseInt(unit);
    }

    @Override
    protected BlockPos deserializeFromUnits(Collection<Integer> units) {
        Integer[] unitsArray = new Integer[3];
        unitsArray = units.toArray(unitsArray);
        return new BlockPos(unitsArray[0], unitsArray[1], unitsArray[2]);
    }

    @Override
    protected JsonToken getUnitToken() {
        return JsonToken.VALUE_NUMBER_INT;
    }
}
