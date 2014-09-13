/**
 * 
 */
package logbook.dto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import logbook.data.context.GlobalContext;
import logbook.internal.EnemyData;
import logbook.internal.Item;
import logbook.internal.Ship;

/**
 * @author Nekopanda
 * １回の会敵の情報をまとめた
 */
public class BattleExDto extends AbstractDto {

    /** 日付 */
    private final Date battleDate;

    /** 味方艦隊 */
    private final List<DockDto> friends = new ArrayList<>();

    /** 敵艦隊 */
    private final List<ShipInfoDto> enemy = new ArrayList<>();

    /** 敵装備 */
    private final List<ItemDto[]> enemySlot = new ArrayList<>();

    /** 敵パラメータ */
    private final List<int[]> enemyParam = new ArrayList<>();

    /** 味方MaxHP */
    private int[] maxFriendHp;

    private int[] maxFriendHpCombined;

    /** 敵MaxHP */
    private int[] maxEnemyHp;

    /** 味方戦闘開始時HP */
    private int[] startFriendHp;

    private int[] startFriendHpCombined;

    /** 敵戦闘開始時HP */
    private int[] startEnemyHp;

    /** 戦闘前の味方総HP */
    private int friendGaugeMax = 0;

    /** 戦闘前の敵総HP */
    private int enemyGaugeMax = 0;

    /** 陣形（味方・敵） */
    private final String[] formation = new String[] { "陣形不明", "陣形不明" };

    /** 同航戦とか　*/
    private String formationMatch = "不明";

    /** 索敵状態（味方・敵） */
    private String sakuteki[];

    /** 海域名 */
    private String questName;

    /** ランク */
    private String rank;

    /** マス */
    private MapCellDto mapCellDto;

    /** 敵艦隊名 */
    private String enemyName;

    /** ドロップフラグ */
    private boolean dropFlag;

    /** 艦種 */
    private String dropType;

    /** 艦名 */
    private String dropName;

    private final List<Phase> phaseList = new ArrayList<Phase>();

    /////////////////////////////////////////////////

    public class Phase {

        private final BattlePhaseKind kind;
        /** 味方HP */
        private final int[] nowFriendHp;

        private final int[] nowFriendHpCombined;

        /** 敵HP */
        private final int[] nowEnemyHp;

        /** ランク */
        private final ResultRank estimatedRank;

        /** 夜戦 */
        private final boolean isNight;

        /** 支援攻撃のタイプ */
        private String supportType;

        /** 接触機（味方・敵） -1の場合は「接触なし」 */
        private int[] touchPlane;

        private String seiku;

        /** 損害率（味方・敵） */
        private double[] damageRate;

        /** 攻撃シーケンス */
        private AirBattleDto air = null;
        private AirBattleDto air2 = null;
        private List<BattleAtackDto> support = null;
        private List<BattleAtackDto> opening = null;
        private List<BattleAtackDto> raigeki = null;
        private List<BattleAtackDto> hougeki = null;
        private List<BattleAtackDto> hougeki1 = null;
        private List<BattleAtackDto> hougeki2 = null;
        private List<BattleAtackDto> hougeki3 = null;

        public Phase(JsonObject object, BattlePhaseKind kind,
                int[] beforeFriendHp, int[] beforeFriendHpCombined, int[] beforeEnemyHp)
        {
            int numFships = beforeFriendHp.length;
            int numEships = beforeEnemyHp.length;
            boolean isCombined = (beforeFriendHpCombined != null);
            int numFshipsCombined = isCombined ? beforeFriendHpCombined.length : 0;

            this.kind = kind;
            this.isNight = kind.isNight();

            this.nowFriendHp = beforeFriendHp.clone();
            this.nowEnemyHp = beforeEnemyHp.clone();
            if (isCombined) {
                this.nowFriendHpCombined = beforeFriendHpCombined.clone();
            }
            else {
                this.nowFriendHpCombined = null;
            }

            // 接触
            JsonArray jsonTouchPlane = object.getJsonArray("api_touch_plane");
            if (jsonTouchPlane != null) {
                this.touchPlane = new int[] {
                        jsonTouchPlane.getInt(0),
                        jsonTouchPlane.getInt(1)
                };
            }

            // 攻撃シーケンスを読み取る //

            // 航空戦（通常）
            JsonObject kouku = object.getJsonObject("api_kouku");
            if (kouku != null) {
                this.air = new AirBattleDto(kouku, isCombined);
                // 昼戦の接触はここ
                this.touchPlane = this.air.touchPlane;
                // 制空はここから取る
                this.seiku = this.air.seiku;
            }

            // 支援艦隊
            JsonNumber support_flag = object.getJsonNumber("api_support_flag");
            if ((support_flag != null) && (support_flag.intValue() != 0)) {
                JsonObject support = object.getJsonObject("api_support_info");
                if (support != null) {
                    JsonObject support_hourai = support.getJsonObject("api_support_hourai");
                    if (support_hourai != null) {
                        JsonArray edam = support_hourai.getJsonArray("api_damage");
                        if (edam != null) {
                            this.support = BattleAtackDto.makeSupport(edam);
                        }
                    }
                }
                this.supportType = toSupport(support_flag.intValue());
            }
            else {
                this.supportType = "";
            }

            // 航空戦（連合艦隊のみ？）
            JsonObject kouku2 = object.getJsonObject("api_kouku2");
            if (kouku2 != null)
                this.air2 = new AirBattleDto(kouku2, isCombined);

            // 開幕
            this.opening = BattleAtackDto.makeRaigeki(object.get("api_opening_atack"), isCombined);

            // 砲撃
            this.hougeki = BattleAtackDto.makeHougeki(object.get("api_hougeki"), (isCombined && this.isNight)); // 夜戦
            this.hougeki1 = BattleAtackDto.makeHougeki(object.get("api_hougeki1"), isCombined);

            // 雷撃
            this.raigeki = BattleAtackDto.makeRaigeki(object.get("api_raigeki"), isCombined);

            // 砲撃（連合艦隊用）
            this.hougeki2 = BattleAtackDto.makeHougeki(object.get("api_hougeki2"), false);
            this.hougeki3 = BattleAtackDto.makeHougeki(object.get("api_hougeki3"), false);

            // ダメージを反映 //

            if (this.air != null)
                this.doAtack(this.air.atacks);
            this.doAtack(this.support);
            if (this.air2 != null)
                this.doAtack(this.air2.atacks);
            this.doAtack(this.opening);
            this.doAtack(this.hougeki);
            this.doAtack(this.hougeki1);
            this.doAtack(this.raigeki);
            this.doAtack(this.hougeki2);
            this.doAtack(this.hougeki3);

            // HP0以下を0にする
            for (int i = 0; i < numFships; i++) {
                if (this.nowFriendHp[i] <= 0)
                    this.nowFriendHp[i] = 0;
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] <= 0)
                    this.nowEnemyHp[i] = 0;
            }
            if (isCombined) {
                for (int i = 0; i < numFshipsCombined; i++) {
                    if (this.nowFriendHpCombined[i] <= 0)
                        this.nowFriendHpCombined[i] = 0;
                }
            }

            // 判定を計算
            this.estimatedRank = this.calcResultRank();
        }

        // 勝利判定 //
        private ResultRank calcResultRank() {
            boolean isCombined = (this.nowFriendHpCombined != null);
            int numFships = this.nowFriendHp.length;
            int numFshipsCombined = isCombined ? this.nowFriendHpCombined.length : 0;
            int numEships = this.nowEnemyHp.length;
            // 戦闘後に残っている艦数
            int friendNowShips = 0;
            int enemyNowShips = 0;
            // 総ダメージ
            int friendGauge = 0;
            int enemyGauge = 0;

            for (int i = 0; i < numFships; i++) {
                if (this.nowFriendHp[i] > 0) {
                    ++friendNowShips;
                }
                friendGauge += BattleExDto.this.getStartFriendHp()[i] - this.nowFriendHp[i];
            }
            if (isCombined) {
                for (int i = 0; i < numFshipsCombined; i++) {
                    if (this.nowFriendHpCombined[i] > 0) {
                        ++friendNowShips;
                    }
                    friendGauge += BattleExDto.this.getStartFriendHpCombined()[i] - this.nowFriendHpCombined[i];
                }
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] > 0)
                    ++enemyNowShips;

                enemyGauge += BattleExDto.this.getStartEnemyHp()[i] - this.nowEnemyHp[i];
            }

            // 轟沈・撃沈数
            int friendSunk = (numFships + numFshipsCombined) - friendNowShips;
            int enemySunk = numEships - enemyNowShips;

            this.damageRate = new double[] {
                    (double) friendGauge / BattleExDto.this.getFriendGaugeMax(),
                    (double) enemyGauge / BattleExDto.this.getEnemyGaugeMax()
            };

            double friendGaugeRate = this.damageRate[0];
            double enemyGaugeRate = this.damageRate[1];
            // 1.1
            boolean equalOrMore = (1.095 * enemyGaugeRate) >= friendGaugeRate;
            boolean equalOrMore1 = (1.12 * enemyGaugeRate) >= friendGaugeRate;
            // 2.5倍以上
            boolean superior = (enemyGaugeRate > 0.01) && (enemyGaugeRate >= (2.53 * friendGaugeRate));
            boolean superior1 = (enemyGaugeRate > 0.01) && (enemyGaugeRate >= (2.5 * friendGaugeRate));
            boolean superior2 = (enemyGaugeRate > 0.01) && (enemyGaugeRate >= (2.3 * friendGaugeRate));

            if (friendSunk == 0) { // 味方轟沈数ゼロ
                if (enemyNowShips == 0) { // 敵を殲滅した
                    if (friendGauge == 0) { // 味方ダメージゼロ
                        return ResultRank.PERFECT;
                    }
                    return ResultRank.S;
                }
                else {
                    // 6隻の場合のみ4隻以上撃沈？
                    if (numEships == 6) {
                        if (enemySunk >= 4) {
                            return ResultRank.A;
                        }
                    }
                    // 半数以上撃沈？
                    else if ((enemySunk * 2) >= numEships) {
                        return ResultRank.A;
                    }
                    // 敵旗艦を撃沈
                    if (this.nowEnemyHp[0] == 0) {
                        return ResultRank.B;
                    }
                    // 戦果ゲージが2.5倍以上
                    if (superior) {
                        return ResultRank.B;
                    }
                    else if (superior1) {
                        return ResultRank.B_OR_C;
                    }
                    else if (superior2) {
                        return ResultRank.C_OR_B;
                    }
                }
            }
            else {
                // 敵を殲滅した
                if (enemyNowShips == 0) {
                    return ResultRank.B;
                }
                // 敵旗艦を撃沈 and 味方轟沈数 < 敵撃沈数
                if ((this.nowEnemyHp[0] == 0) && (friendSunk < enemySunk)) {
                    return ResultRank.B;
                }
                // 戦果ゲージが2.5倍以上
                if (superior) {
                    return ResultRank.B;
                }
                else if (superior1) {
                    return ResultRank.B_OR_C;
                }
                else if (superior2) {
                    return ResultRank.C_OR_B;
                }
                // 敵旗艦を撃沈
                if (this.nowEnemyHp[0] == 0) {
                    return ResultRank.C;
                }
            }
            // 敵に与えたダメージが一定以上 and 戦果ゲージが1.0倍以上
            if (enemyGauge > 0) {
                if (equalOrMore) {
                    return ResultRank.C;
                }
                else if (equalOrMore1) {
                    return ResultRank.D_OR_C;
                }
            }
            // 轟沈艦があり かつ 残った艦が１隻のみ
            if ((friendSunk > 0) && ((numFships - friendSunk) == 1)) {
                return ResultRank.E;
            }
            // 残りはD
            return ResultRank.D;
        }

        // ダメージを反映
        private void doAtack(List<BattleAtackDto> seq) {
            if (seq == null)
                return;

            for (BattleAtackDto dto : seq) {
                for (int i = 0; i < dto.target.length; ++i) {
                    int target = dto.target[i];
                    int damage = dto.damage[i];
                    if (dto.friendAtack) {
                        this.nowEnemyHp[target] -= damage;
                    }
                    else {
                        if (target < 6) {
                            this.nowFriendHp[target] -= damage;
                        }
                        else {
                            this.nowFriendHpCombined[target - 6] -= damage;
                        }
                    }
                }
            }
        }

        public boolean isCombined() {
            return (this.nowFriendHpCombined != null);
        }

        public AirBattleDto[] getAirBattleDto() {
            return new AirBattleDto[] {
                    this.air, this.air2
            };
        }

        private BattleAtackDto[] toArray(List<BattleAtackDto> list) {
            return list.toArray(new BattleAtackDto[list.size()]);
        }

        public BattleAtackDto[][] getAtackSequence() {
            return new BattleAtackDto[][] {
                    ((this.air == null) || (this.air.atacks == null)) ? null :
                            this.toArray(this.air.atacks),
                    this.support == null ? null : this.toArray(this.support),
                    ((this.air2 == null) || (this.air2.atacks == null)) ? null :
                            this.toArray(this.air2.atacks),
                    this.opening == null ? null : this.toArray(this.opening),
                    this.hougeki == null ? null : this.toArray(this.hougeki),
                    this.hougeki1 == null ? null : this.toArray(this.hougeki1),
                    this.raigeki == null ? null : this.toArray(this.raigeki),
                    this.hougeki2 == null ? null : this.toArray(this.hougeki2),
                    this.hougeki3 == null ? null : this.toArray(this.hougeki3),
            };
        }

        public String getRankCalcInfo() {
            boolean isCombined = (this.nowFriendHpCombined != null);
            int numFships = this.nowFriendHp.length;
            int numFshipsCombined = isCombined ? this.nowFriendHpCombined.length : 0;
            int numEships = this.nowEnemyHp.length;
            // 戦闘後に残っている艦数
            int friendNowShips = 0;
            int enemyNowShips = 0;
            // 総ダメージ
            int friendGauge = 0;
            int enemyGauge = 0;

            for (int i = 0; i < numFships; i++) {
                if (this.nowFriendHp[i] > 0) {
                    ++friendNowShips;
                }
                friendGauge += BattleExDto.this.getStartFriendHp()[i] - this.nowFriendHp[i];

            }
            if (isCombined) {
                for (int i = 0; i < numFshipsCombined; i++) {
                    if (this.nowFriendHpCombined[i] > 0) {
                        ++friendNowShips;
                    }
                    friendGauge += BattleExDto.this.getStartFriendHpCombined()[i] - this.nowFriendHpCombined[i];
                }
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] > 0)
                    ++enemyNowShips;

                enemyGauge += BattleExDto.this.getStartEnemyHp()[i] - this.nowEnemyHp[i];
            }

            //double enemyGaugeRate = (double) enemyGauge / this.enemyGaugeMax;
            //double friendGaugeRate = (double) friendGauge / this.friendGaugeMax;

            return "味方[艦:" + (numFships + numFshipsCombined) + "→" + friendNowShips + " ゲージ:" +
                    friendGauge + "/" + BattleExDto.this.getFriendGaugeMax() +
                    "] 敵[艦:" + this.nowEnemyHp.length + "→" + enemyNowShips + " ゲージ:" + enemyGauge + "/"
                    + BattleExDto.this.getEnemyGaugeMax() +
                    "]" +
                    //"(" + (enemyGaugeRate / friendGaugeRate) + "/" + (friendGaugeRate / enemyGaugeRate) + ") " +
                    "判定:" + this.estimatedRank.rank();
        }

        /**
         * @return kind
         */
        public BattlePhaseKind getKind() {
            return this.kind;
        }

        /**
         * @return nowFriendHp
         */
        public int[] getNowFriendHp() {
            return this.nowFriendHp;
        }

        /**
         * @return nowFriendHpCombined
         */
        public int[] getNowFriendHpCombined() {
            return this.nowFriendHpCombined;
        }

        /**
         * @return nowEnemyHp
         */
        public int[] getNowEnemyHp() {
            return this.nowEnemyHp;
        }

        /**
         * @return estimatedRank
         */
        public ResultRank getEstimatedRank() {
            return this.estimatedRank;
        }

        /**
         * @return isNight
         */
        public boolean isNight() {
            return this.isNight;
        }

        /**
         * @return supportType
         */
        public String getSupportType() {
            return this.supportType;
        }

        /**
         * @return touchPlane
         */
        public int[] getTouchPlane() {
            return this.touchPlane;
        }

        /**
         * @return seiku
         */
        public String getSeiku() {
            return this.seiku;
        }

        /**
         * @return damageRate
         */
        public double[] getDamageRate() {
            return this.damageRate;
        }

        /**
         * @return air
         */
        public AirBattleDto getAir() {
            return this.air;
        }

        /**
         * @return air2
         */
        public AirBattleDto getAir2() {
            return this.air2;
        }

        /**
         * @return support
         */
        public List<BattleAtackDto> getSupport() {
            return this.support;
        }

        /**
         * @return opening
         */
        public List<BattleAtackDto> getOpening() {
            return this.opening;
        }

        /**
         * @return raigeki
         */
        public List<BattleAtackDto> getRaigeki() {
            return this.raigeki;
        }

        /**
         * @return hougeki
         */
        public List<BattleAtackDto> getHougeki() {
            return this.hougeki;
        }

        /**
         * @return hougeki1
         */
        public List<BattleAtackDto> getHougeki1() {
            return this.hougeki1;
        }

        /**
         * @return hougeki2
         */
        public List<BattleAtackDto> getHougeki2() {
            return this.hougeki2;
        }

        /**
         * @return hougeki3
         */
        public List<BattleAtackDto> getHougeki3() {
            return this.hougeki3;
        }
    }

    public BattleExDto() {
        this.battleDate = Calendar.getInstance().getTime();
    }

    public Phase addPhase(JsonObject object, BattlePhaseKind kind) {
        if (this.phaseList.size() == 0) {
            // 最初のフェーズ
            String dockId;

            if (object.containsKey("api_dock_id")) {
                dockId = object.get("api_dock_id").toString();
            } else {
                dockId = object.get("api_deck_id").toString();
            }

            JsonArray nowhps = object.getJsonArray("api_nowhps");
            JsonArray maxhps = object.getJsonArray("api_maxhps");
            JsonArray nowhpsCombined = object.getJsonArray("api_nowhps_combined");
            JsonArray maxhpsCombined = object.getJsonArray("api_maxhps_combined");
            boolean isCombined = (nowhpsCombined != null);

            int numFships = 6;
            int numFshipsCombined = 0;

            for (int i = 1; i <= 6; ++i) {
                if (maxhps.getInt(i) == -1) {
                    numFships = i - 1;
                    break;
                }
            }
            this.friends.add(GlobalContext.getDock(dockId));
            if (object.containsKey("api_fParam_combined")) {
                this.friends.add(GlobalContext.getDock("2"));
                numFshipsCombined = 6;
                for (int i = 1; i <= 6; ++i) {
                    if (maxhpsCombined.getInt(i) == -1) {
                        numFshipsCombined = i - 1;
                        break;
                    }
                }
            }

            JsonArray shipKe = object.getJsonArray("api_ship_ke");
            for (int i = 1; i < shipKe.size(); i++) {
                long id = shipKe.getJsonNumber(i).longValue();
                ShipInfoDto dto = Ship.get(Long.toString(id));
                if (dto != null) {
                    this.enemy.add(dto);
                }
            }
            int numEships = this.enemy.size();

            JsonArray eSlots = object.getJsonArray("api_eSlot");
            JsonArray eParams = object.getJsonArray("api_eParam");
            for (int i = 0; i < this.enemy.size(); i++) {
                JsonArray eSlot = eSlots.getJsonArray(i);
                ItemDto[] slot = new ItemDto[5];
                for (int j = 0; j < eSlot.size(); j++) {
                    slot[j] = Item.get(eSlot.getInt(j));
                }
                this.enemySlot.add(slot);
                JsonArray eParam = eParams.getJsonArray(i);
                int[] param = new int[4];
                for (int j = 0; j < eParam.size(); j++) {
                    param[j] = eParam.getInt(j);
                }
                this.enemyParam.add(param);
            }

            this.startFriendHp = new int[numFships];
            this.startEnemyHp = new int[numEships];
            this.maxFriendHp = new int[numFships];
            this.maxEnemyHp = new int[numEships];
            if (isCombined) {
                this.startFriendHpCombined = new int[numFshipsCombined];
                this.maxFriendHpCombined = new int[numFshipsCombined];
            }
            else {
                this.maxFriendHpCombined = null;
            }

            // 陣形
            if (object.containsKey("api_formation")) {
                JsonArray formation = object.getJsonArray("api_formation");
                for (int i = 0; i < 2; ++i) {
                    switch (formation.get(i).getValueType()) {
                    case NUMBER:
                        this.formation[i] = toFormation(formation.getInt(i));
                        break;
                    default:
                        this.formation[i] = toFormation(Integer.parseInt(formation.getString(i)));
                    }
                }
                this.formationMatch = toMatch(formation.getInt(2));
            }

            // 索敵
            JsonArray jsonSearch = object.getJsonArray("api_search");
            if (jsonSearch != null) {
                this.sakuteki = new String[] {
                        toSearch(jsonSearch.getInt(0)),
                        toSearch(jsonSearch.getInt(1))
                };
            }

            // この戦闘の開始前HPを取得
            for (int i = 1; i < nowhps.size(); i++) {
                int hp = nowhps.getInt(i);
                int maxHp = maxhps.getInt(i);
                if (i <= 6) {
                    if (i <= numFships) {
                        this.maxFriendHp[i - 1] = maxHp;
                        this.friendGaugeMax += this.startFriendHp[i - 1] = hp;
                    }
                } else {
                    if ((i - 6) <= numEships) {
                        this.maxEnemyHp[i - 1 - 6] = maxHp;
                        this.enemyGaugeMax += this.startEnemyHp[i - 1 - 6] = hp;
                    }
                }
            }
            if (isCombined) {
                for (int i = 1; i < nowhpsCombined.size(); i++) {
                    int hp = nowhpsCombined.getInt(i);
                    int maxHp = maxhpsCombined.getInt(i);
                    if (i <= numFshipsCombined) {
                        this.maxFriendHpCombined[i - 1] = maxHp;
                        this.friendGaugeMax += this.startFriendHpCombined[i - 1] = hp;
                    }
                }
            }
        }

        if (this.phaseList.size() > 0) {
            Phase phase = this.phaseList.get(0);
            this.phaseList.add(new Phase(object, kind,
                    phase.getNowFriendHp(), phase.getNowFriendHpCombined(), phase.getNowEnemyHp()));
        }
        else {
            this.phaseList.add(new Phase(object, kind,
                    this.startFriendHp, this.startFriendHpCombined, this.startEnemyHp));
        }

        return this.phaseList.get(this.phaseList.size() - 1);
    }

    public void setResult(JsonObject object, MapCellDto mapInfo) {
        if (object.get("api_quest_name") != null) {
            this.questName = object.getString("api_quest_name");
        }
        else {
            // 演習の場合はない
            this.questName = null;
        }
        this.rank = object.getString("api_win_rank");
        this.mapCellDto = mapInfo;
        this.enemyName = object.getJsonObject("api_enemy_info").getString("api_deck_name");
        this.dropFlag = object.containsKey("api_get_ship");
        if (this.isDropFlag()) {
            this.dropType = object.getJsonObject("api_get_ship").getString("api_ship_type");
            this.dropName = object.getJsonObject("api_get_ship").getString("api_ship_name");
        } else {
            this.dropType = "";
            this.dropName = "";
        }
    }

    private static String toFormation(int f) {
        String formation;
        switch (f) {
        case 1:
            formation = "単縦陣";
            break;
        case 2:
            formation = "複縦陣";
            break;
        case 3:
            formation = "輪形陣";
            break;
        case 4:
            formation = "梯形陣";
            break;
        case 5:
            formation = "単横陣";
            break;
        case 11:
            formation = "第一警戒航行序列";
            break;
        case 12:
            formation = "第二警戒航行序列";
            break;
        case 13:
            formation = "第三警戒航行序列";
            break;
        case 14:
            formation = "第四警戒航行序列";
            break;
        default:
            formation = "単縦陣";
            break;
        }
        return formation;
    }

    private static String toMatch(int id) {
        switch (id) {
        case 1:
            return "同航戦";
        case 2:
            return "反航戦";
        case 3:
            return "Ｔ字有利";
        case 4:
            return "Ｔ字不利";
        default:
            return "不明(" + id + ")";
        }
    }

    private static String toSupport(int id) {
        switch (id) {
        case 1:
            return "航空支援";
        case 2:
            return "支援射撃";
        case 3:
            return "支援長距離雷撃";
        default:
            return "不明(" + id + ")";
        }
    }

    private static String toSearch(int id) {
        switch (id) {
        case 1:
            return "発見!";
        case 2:
            return "発見!索敵機未帰還機あり";
        case 3:
            return "発見できず…索敵機未帰還機あり";
        case 4:
            return "発見できず…";
        case 5:
            return "発見!(索敵機なし)";
        case 6:
            return "なし";
        default:
            return "不明(" + id + ")";
        }
    }

    public EnemyData getEnemyData(int enemyId, String enemyName) {
        String[] enemyShips = new String[] { "", "", "", "", "", "" };
        for (int i = 0; i < this.enemy.size(); ++i) {
            enemyShips[i] = this.enemy.get(i).getEnemyShipName();
        }
        return new EnemyData(enemyId, enemyName, enemyShips, this.formation[1]);
    }

    public boolean isCombined() {
        return (this.startFriendHpCombined != null);
    }

    /** 最後に行ったフェーズを取得 */
    public Phase getLastPhase() {
        if (this.phaseList.size() == 0)
            return null;
        return this.phaseList.get(this.phaseList.size() - 1);
    }

    /** 最初のフェーズ */
    public Phase getPhase1() {
        if (this.phaseList.size() < 1)
            return null;
        return this.phaseList.get(0);
    }

    /** ２番目のフェーズ（ない時はnull） */
    public Phase getPhase2() {
        if (this.phaseList.size() < 2)
            return null;
        return this.phaseList.get(1);
    }

    /** 戦闘結果も含んでいるか */
    public boolean isCompleteResult() {
        return (this.rank != null) && (this.phaseList.size() > 0);
    }

    /** 演習ではなく出撃か */
    public boolean isCompleteSortieBattle() {
        return (this.questName != null) && (this.mapCellDto != null) && this.isCompleteResult();
    }

    /** 交戦後のHP */
    public int[] getNowFriendHp() {
        return this.getLastPhase().getNowFriendHp();
    }

    /** 交戦後のHP（連合艦隊でないときはnull） */
    public int[] getNowFriendHpCombined() {
        return this.getLastPhase().getNowFriendHpCombined();
    }

    /** 交戦後のHP */
    public int[] getNowEnemyHp() {
        return this.getLastPhase().getNowEnemyHp();
    }

    /** 味方艦隊（連合艦隊の時は第一艦隊） */
    public DockDto getDock() {
        return this.friends.get(0);
    }

    /** 連合艦隊（連合艦隊でないときはnull） */
    public DockDto getDockCombined() {
        if (this.friends.size() < 2)
            return null;
        return this.friends.get(1);
    }

    /**
     * @return battleDate
     */
    public Date getBattleDate() {
        return this.battleDate;
    }

    /**
     * @return friends
     */
    public List<DockDto> getFriends() {
        return this.friends;
    }

    /**
     * @return enemy
     */
    public List<ShipInfoDto> getEnemy() {
        return this.enemy;
    }

    /**
     * @return enemySlot
     */
    public List<ItemDto[]> getEnemySlot() {
        return this.enemySlot;
    }

    /**
     * @return enemyParam
     */
    public List<int[]> getEnemyParam() {
        return this.enemyParam;
    }

    /**
     * @return maxFriendHp
     */
    public int[] getMaxFriendHp() {
        return this.maxFriendHp;
    }

    /**
     * @return maxFriendHpCombined
     */
    public int[] getMaxFriendHpCombined() {
        return this.maxFriendHpCombined;
    }

    /**
     * @return maxEnemyHp
     */
    public int[] getMaxEnemyHp() {
        return this.maxEnemyHp;
    }

    /**
     * @return startFriendHp
     */
    public int[] getStartFriendHp() {
        return this.startFriendHp;
    }

    /**
     * @return startFriendHpCombined
     */
    public int[] getStartFriendHpCombined() {
        return this.startFriendHpCombined;
    }

    /**
     * @return startEnemyHp
     */
    public int[] getStartEnemyHp() {
        return this.startEnemyHp;
    }

    /**
     * @return friendGaugeMax
     */
    public int getFriendGaugeMax() {
        return this.friendGaugeMax;
    }

    /**
     * @return enemyGaugeMax
     */
    public int getEnemyGaugeMax() {
        return this.enemyGaugeMax;
    }

    /**
     * @return formation
     */
    public String[] getFormation() {
        return this.formation;
    }

    /**
     * @return formationMatch
     */
    public String getFormationMatch() {
        return this.formationMatch;
    }

    /**
     * @return sakuteki
     */
    public String[] getSakuteki() {
        return this.sakuteki;
    }

    /**
     * @return questName
     */
    public String getQuestName() {
        return this.questName;
    }

    /**
     * @return rank
     */
    public String getRank() {
        return this.rank;
    }

    /**
     * @return mapCelldto
     */
    public MapCellDto getMapCellDto() {
        return this.mapCellDto;
    }

    /**
     * @return enemyName
     */
    public String getEnemyName() {
        return this.enemyName;
    }

    /**
     * @return dropFlag
     */
    public boolean isDropFlag() {
        return this.dropFlag;
    }

    /**
     * @return dropType
     */
    public String getDropType() {
        return this.dropType;
    }

    /**
     * @return dropName
     */
    public String getDropName() {
        return this.dropName;
    }

    /**
     * @return phaseList
     */
    public List<Phase> getPhaseList() {
        return this.phaseList;
    }
}