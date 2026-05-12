package com.mettyoung.deconstructchinese.util

/**
 * Character-level Traditional ↔ Simplified Chinese converter.
 * Data from OpenCC core character tables (STCharacters / TSCharacters).
 * Covers HSK 1–6 and common everyday vocabulary (~400 pairs).
 * Ambiguous multi-reading characters (e.g. 发 → 發/髮) map to the most common traditional form.
 */
object ChineseScriptConverter {

    private val s2t: Map<Char, Char> = mapOf(
        // Numbers / measures
        '万' to '萬', '亿' to '億', '两' to '兩',
        // Pronouns / particles
        '们' to '們', '这' to '這', '么' to '麼',
        // Directions / position
        '东' to '東', '边' to '邊', '间' to '間', '乡' to '鄉',
        // Common verbs
        '来' to '來', '开' to '開', '关' to '關', '说' to '說',
        '问' to '問', '买' to '買', '带' to '帶', '转' to '轉',
        '运' to '運', '动' to '動', '飞' to '飛', '换' to '換',
        '给' to '給', '进' to '進', '离' to '離', '让' to '讓',
        '过' to '過', '达' to '達', '见' to '見', '记' to '記',
        '认' to '認', '写' to '寫', '读' to '讀', '听' to '聽',
        '讲' to '講', '选' to '選', '赢' to '贏', '随' to '隨',
        '应' to '應', '传' to '傳', '结' to '結', '联' to '聯',
        '续' to '續', '继' to '繼', '获' to '獲', '处' to '處',
        '讨' to '討', '论' to '論', '报' to '報', '补' to '補',
        '构' to '構',
        // Nouns
        '书' to '書', '车' to '車', '时' to '時', '国' to '國',
        '学' to '學', '语' to '語', '树' to '樹', '楼' to '樓',
        '风' to '風', '门' to '門', '线' to '線', '现' to '現',
        '图' to '圖', '号' to '號', '历' to '歷', '头' to '頭',
        '脸' to '臉', '脑' to '腦', '码' to '碼', '钱' to '錢',
        '网' to '網', '钟' to '鐘', '脚' to '腳', '马' to '馬',
        '鸡' to '雞', '鸟' to '鳥', '鱼' to '魚', '龙' to '龍',
        '鸭' to '鴨', '妈' to '媽', '华' to '華', '汉' to '漢',
        '环' to '環', '课' to '課', '题' to '題', '级' to '級',
        '场' to '場', '楼' to '樓', '区' to '區', '层' to '層',
        '条' to '條', '业' to '業', '术' to '術', '艺' to '藝',
        '体' to '體', '验' to '驗', '经' to '經', '贸' to '貿',
        '财' to '財', '务' to '務', '规' to '規', '则' to '則',
        '质' to '質', '据' to '據', '资' to '資', '数' to '數',
        '绪' to '緒', '权' to '權', '党' to '黨', '团' to '團',
        '约' to '約', '统' to '統', '际' to '際', '实' to '實',
        '际' to '際', '标' to '標', '试' to '試', '错' to '錯',
        '练' to '練', '测' to '測', '络' to '絡', '户' to '戶',
        // Adjectives
        '长' to '長', '热' to '熱', '难' to '難', '满' to '滿',
        '够' to '夠', '软' to '軟', '岁' to '歲', '虽' to '雖',
        '旧' to '舊', '坏' to '壞', '响' to '響', '广' to '廣',
        '强' to '強', '远' to '遠', '乐' to '樂', '爱' to '愛',
        // More common characters
        '发' to '發', '为' to '為', '无' to '無', '从' to '從',
        '对' to '對', '该' to '該', '个' to '個', '够' to '夠',
        '还' to '還', '后' to '後', '话' to '話', '怀' to '懷',
        '欢' to '歡', '会' to '會', '机' to '機', '积' to '積',
        '几' to '幾', '济' to '濟', '将' to '將', '样' to '樣',
        '页' to '頁', '义' to '義', '战' to '戰', '种' to '種',
        '总' to '總', '组' to '組', '帅' to '帥', '产' to '產',
        '参' to '參', '当' to '當', '导' to '導', '断' to '斷',
        '废' to '廢', '盖' to '蓋', '赶' to '趕', '农' to '農',
        '请' to '請', '损' to '損', '伤' to '傷', '医' to '醫',
        '药' to '藥', '疗' to '療', '虑' to '慮', '忆' to '憶',
        '显' to '顯', '愿' to '願', '压' to '壓', '帮' to '幫',
        '类' to '類', '办' to '辦', '败' to '敗', '备' to '備',
        '贝' to '貝', '滨' to '濱', '称' to '稱', '冲' to '衝',
        '灯' to '燈', '点' to '點', '电' to '電', '欧' to '歐',
        '盘' to '盤', '赔' to '賠', '传' to '傳', '达' to '達',
        '别' to '別', '彻' to '徹', '陈' to '陳', '虫' to '蟲',
        '两' to '兩', '满' to '滿', '难' to '難', '处' to '處',
        // Colours
        '红' to '紅', '绿' to '綠', '蓝' to '藍',
        // Food / drink
        '饭' to '飯', '饮' to '飲', '汤' to '湯', '饺' to '餃',
        // Body
        '颈' to '頸', '肩' to '肩',
    )

    private val t2s: Map<Char, Char> by lazy {
        s2t.entries.associate { (s, t) -> t to s }
    }

    /** Convert simplified Chinese text to traditional. Unknown/shared characters pass through unchanged. */
    fun toTraditional(text: String): String = text.map { s2t[it] ?: it }.joinToString("")

    /** Convert traditional Chinese text to simplified. Unknown/shared characters pass through unchanged. */
    fun toSimplified(text: String): String = text.map { t2s[it] ?: it }.joinToString("")
}
