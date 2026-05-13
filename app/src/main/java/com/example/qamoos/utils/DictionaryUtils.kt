package com.example.qamoos.utils

object DictionaryUtils {
    private val nativeNames = mapOf(
        "taj" to "تاج العروس",
        "lisanularab" to "لسان العرب",
        "asaas" to "أساس البلاغة",
        "mujam_ghani" to "معجم الغني",
        "mujamul_shihah" to "معجم الصحاح",
        "mujamul_waseet" to "المعجم الوسيط",
        "qamoos_moheet" to "القاموس المحيط",
        "sehah" to "الصحاح في اللغة",
        "aalam" to "معجم الأعلام",
        "afaal" to "معجم الأفعال",
        "altibi" to "المعجم الطبي",
        "amthal" to "مجمع الأمثال",
        "blaghah_dict" to "معجم البلاغة",
        "boldan" to "معجم البلدان",
        "frooq" to "معجم الفروق",
        "maany" to "معجم المعاني",
        "maqayys" to "مقاييس اللغة",
        "misbah" to "المصباح المنير",
        "muasirah" to "اللغة المعاصرة",
        "mufradatquran" to "مفردات القرآن",
        "mutradef" to "معجم المترادفات",
        "nahwa" to "معجم النحو",
        "nehayah" to "النهاية في غريب الحديث",
        "phikhy1" to "المعجم الفقهي",
        "sarf" to "معجم الصرف",
        "tarefaat" to "كتاب التعريفات",
        "aashab" to "معجم الأعشاب",
        "clothes" to "معجم الملابس",
        "maalem" to "معجم المعالم",
        "maany_dict" to "معجم المعاني",
        "irab" to "معجم الإعراب",
        "english" to "English to Ar/Ml",
        "malayalam" to "Malayalam to Ar/En",
        "arabic" to "Arabic to Ml/En",
        "taj al-arus" to "تاج العروس",
        "lisan al-arab" to "لسان العرب",
        "mujam al-aashab" to "معجم الأعشاب",
        "dictionary of clothes" to "معجم الملابس",
        "mujam al-maalem" to "معجم المعالم",
        "mujam al-maany (dict)" to "معجم المعاني",
        "mujam al-maany" to "معجم المعاني"
    )

    fun getNativeName(tableName: String?, defaultName: String?): String {
        val tableKey = tableName?.lowercase()
        val defaultKey = defaultName?.lowercase()
        
        return nativeNames[tableKey] 
            ?: nativeNames[defaultKey] 
            ?: defaultName 
            ?: tableName 
            ?: ""
    }
}
