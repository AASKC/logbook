/**
 * 
 */
package logbook.dto;

import java.util.Map;

import logbook.internal.Item;

import com.dyuproject.protostuff.Tag;

/**
 * @author Nekopanda
 *
 */
public class EnemyShipDto extends ShipBaseDto {

    /** 火力 */
    @Tag(10)
    private final int karyoku;

    /** 雷装 */
    @Tag(11)
    private final int raisou;

    /** 対空 */
    @Tag(12)
    private final int taiku;

    /** 装甲 */
    @Tag(13)
    private final int soukou;

    /** レベル */
    @Tag(14)
    private final int lv;

    public EnemyShipDto(int shipId, int[] slot, int[] param, int lv) {
        super(shipId, slot);
        this.karyoku = param[0];
        this.raisou = param[0];
        this.taiku = param[0];
        this.soukou = param[0];
        this.lv = lv;
    }

    /**
     * @return 火力
     */
    @Override
    public int getKaryoku() {
        return this.karyoku;
    }

    /**
     * @return 雷装
     */
    @Override
    public int getRaisou() {
        return this.raisou;
    }

    /**
     * @return 対空
     */
    @Override
    public int getTaiku() {
        return this.taiku;
    }

    /**
     * @return 装甲
     */
    @Override
    public int getSoukou() {
        return this.soukou;
    }

    @Override
    public int getLv() {
        return this.lv;
    }

    @Override
    protected Map<Integer, ItemDto> getItemMap() {
        return Item.getMap();
    }

}
