package com.example.qamoos.utils

object DictionaryMetadata {
    private val sizes = mapOf(
        "aalam" to "15 MB",
        "aashab" to "772 KB",
        "afaal" to "2.0 MB",
        "altibi" to "6.9 MB",
        "amthal" to "3.4 MB",
        "arabic" to "24 MB",
        "asaas" to "3.7 MB",
        "blaghah_dict" to "2.9 MB",
        "boldan" to "12 MB",
        "clothes" to "1.4 MB",
        "english" to "7.3 MB",
        "frooq" to "1.2 MB",
        "irab" to "1.3 MB",
        "lisanularab" to "42 MB",
        "maalem" to "876 KB",
        "maany" to "1.3 MB",
        "maany_dict" to "6.2 MB",
        "malayalam" to "23 MB",
        "maqayys" to "5.1 MB",
        "misbah" to "2.7 MB",
        "muasirah" to "15 MB",
        "mufradatquran" to "3.2 MB",
        "mujam_ghani" to "15 MB",
        "mujamul_shihah" to "2.3 MB",
        "mujamul_waseet" to "9.4 MB",
        "mutradef" to "672 KB",
        "nahwa" to "5.3 MB",
        "nehayah" to "8.9 MB",
        "phikhy1" to "4.0 MB",
        "qamoos_moheet" to "12 MB",
        "sarf" to "1.5 MB",
        "sehah" to "6.5 MB",
        "taj" to "51 MB",
        "tarefaat" to "736 KB"
    )

    fun getSize(tableName: String?): String {
        return sizes[tableName?.lowercase()] ?: "Unknown"
    }
}
