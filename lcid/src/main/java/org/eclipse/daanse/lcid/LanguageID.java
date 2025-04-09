/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.lcid;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum LanguageID {

    LANG_0001((short) 0x0001, "ar", "ar"), LANG_0002((short) 0x0002, "bg", "bg"), LANG_0003((short) 0x0003, "ca", "ca"),
    LANG_0004((short) 0x0004, "zh_hans", "zh-Hans"), LANG_0005((short) 0x0005, "cs", "cs"),
    LANG_0006((short) 0x0006, "da", "da"), LANG_0007((short) 0x0007, "de", "de"), LANG_0008((short) 0x0008, "el", "el"),
    LANG_0009((short) 0x0009, "en", "en"), LANG_000A((short) 0x000A, "es", "es"), LANG_000B((short) 0x000B, "fi", "fi"),
    LANG_000C((short) 0x000C, "fr", "fr"), LANG_000D((short) 0x000D, "he", "he"), LANG_000E((short) 0x000E, "hu", "hu"),
    LANG_000F((short) 0x000F, "is", "is"), LANG_0010((short) 0x0010, "it", "it"), LANG_0011((short) 0x0011, "ja", "ja"),
    LANG_0012((short) 0x0012, "ko", "ko"), LANG_0013((short) 0x0013, "nl", "nl"), LANG_0014((short) 0x0014, "no", "no"),
    LANG_0015((short) 0x0015, "pl", "pl"), LANG_0016((short) 0x0016, "pt", "pt"), LANG_0017((short) 0x0017, "rm", "rm"),
    LANG_0018((short) 0x0018, "ro", "ro"), LANG_0019((short) 0x0019, "ru", "ru"), LANG_001A((short) 0x001A, "hr", "hr"),
    LANG_001B((short) 0x001B, "sk", "sk"), LANG_001C((short) 0x001C, "sq", "sq"), LANG_001D((short) 0x001D, "sv", "sv"),
    LANG_001E((short) 0x001E, "th", "th"), LANG_001F((short) 0x001F, "tr", "tr"), LANG_0020((short) 0x0020, "ur", "ur"),
    LANG_0021((short) 0x0021, "id", "id"), LANG_0022((short) 0x0022, "uk", "uk"), LANG_0023((short) 0x0023, "be", "be"),
    LANG_0024((short) 0x0024, "sl", "sl"), LANG_0025((short) 0x0025, "et", "et"), LANG_0026((short) 0x0026, "lv", "lv"),
    LANG_0027((short) 0x0027, "lt", "lt"), LANG_0028((short) 0x0028, "tg", "tg"), LANG_0029((short) 0x0029, "fa", "fa"),
    LANG_002A((short) 0x002A, "vi", "vi"), LANG_002B((short) 0x002B, "hy", "hy"), LANG_002C((short) 0x002C, "az", "az"),
    LANG_002D((short) 0x002D, "eu", "eu"), LANG_002E((short) 0x002E, "hsb", "hsb"),
    LANG_002F((short) 0x002F, "mk", "mk"), LANG_0030((short) 0x0030, "st", "st"), LANG_0031((short) 0x0031, "ts", "ts"),
    LANG_0032((short) 0x0032, "tn", "tn"), LANG_0033((short) 0x0033, "ve", "ve"), LANG_0034((short) 0x0034, "xh", "xh"),
    LANG_0035((short) 0x0035, "zu", "zu"), LANG_0036((short) 0x0036, "af", "af"), LANG_0037((short) 0x0037, "ka", "ka"),
    LANG_0038((short) 0x0038, "fo", "fo"), LANG_0039((short) 0x0039, "hi", "hi"), LANG_003A((short) 0x003A, "mt", "mt"),
    LANG_003B((short) 0x003B, "se", "se"), LANG_003C((short) 0x003C, "ga", "ga"), LANG_003D((short) 0x003D, "yi", "yi"),
    LANG_003E((short) 0x003E, "ms", "ms"), LANG_003F((short) 0x003F, "kk", "kk"), LANG_0040((short) 0x0040, "ky", "ky"),
    LANG_0041((short) 0x0041, "sw", "sw"), LANG_0042((short) 0x0042, "tk", "tk"), LANG_0043((short) 0x0043, "uz", "uz"),
    LANG_0044((short) 0x0044, "tt", "tt"), LANG_0045((short) 0x0045, "bn", "bn"), LANG_0046((short) 0x0046, "pa", "pa"),
    LANG_0047((short) 0x0047, "gu", "gu"), LANG_0048((short) 0x0048, "or", "or"), LANG_0049((short) 0x0049, "ta", "ta"),
    LANG_004A((short) 0x004A, "te", "te"), LANG_004B((short) 0x004B, "kn", "kn"), LANG_004C((short) 0x004C, "ml", "ml"),
    LANG_004D((short) 0x004D, "as", "as"), LANG_004E((short) 0x004E, "mr", "mr"), LANG_004F((short) 0x004F, "sa", "sa"),
    LANG_0050((short) 0x0050, "mn", "mn"), LANG_0051((short) 0x0051, "bo", "bo"), LANG_0052((short) 0x0052, "cy", "cy"),
    LANG_0053((short) 0x0053, "km", "km"), LANG_0054((short) 0x0054, "lo", "lo"), LANG_0055((short) 0x0055, "my", "my"),
    LANG_0056((short) 0x0056, "gl", "gl"), LANG_0057((short) 0x0057, "kok", "kok"),
    LANG_0058((short) 0x0058, "mni", "mni"), LANG_0059((short) 0x0059, "sd", "sd"),
    LANG_005A((short) 0x005A, "syr", "syr"), LANG_005B((short) 0x005B, "si", "si"),
    LANG_005C((short) 0x005C, "chr", "chr"), LANG_005D((short) 0x005D, "iu", "iu"),
    LANG_005E((short) 0x005E, "am", "am"), LANG_005F((short) 0x005F, "tzm", "tzm"),
    LANG_0060((short) 0x0060, "ks", "ks"), LANG_0061((short) 0x0061, "ne", "ne"), LANG_0062((short) 0x0062, "fy", "fy"),
    LANG_0063((short) 0x0063, "ps", "ps"), LANG_0064((short) 0x0064, "fil", "fil"),
    LANG_0065((short) 0x0065, "dv", "dv"), LANG_0066((short) 0x0066, "bin", "bin"),
    LANG_0067((short) 0x0067, "ff", "ff"), LANG_0068((short) 0x0068, "ha", "ha"),
    LANG_0069((short) 0x0069, "ibb", "ibb"), LANG_006A((short) 0x006A, "yo", "yo"),
    LANG_006B((short) 0x006B, "quz", "quz"), LANG_006C((short) 0x006C, "nso", "nso"),
    LANG_006D((short) 0x006D, "ba", "ba"), LANG_006E((short) 0x006E, "lb", "lb"), LANG_006F((short) 0x006F, "kl", "kl"),
    LANG_0070((short) 0x0070, "ig", "ig"), LANG_0071((short) 0x0071, "kr", "kr"), LANG_0072((short) 0x0072, "om", "om"),
    LANG_0073((short) 0x0073, "ti", "ti"), LANG_0074((short) 0x0074, "gn", "gn"),
    LANG_0075((short) 0x0075, "haw", "haw"), LANG_0076((short) 0x0076, "la", "la"),
    LANG_0077((short) 0x0077, "so", "so"), LANG_0078((short) 0x0078, "ii", "ii"),
    LANG_0079((short) 0x0079, "pap", "pap"), LANG_007A((short) 0x007A, "arn", "arn"),
    LANG_007B((short) 0x007B, null, null), LANG_007C((short) 0x007C, "moh", "moh"),
    LANG_007D((short) 0x007D, null, null), LANG_007E((short) 0x007E, "br", "br"), LANG_007F((short) 0x007F, null, null),
    LANG_0080((short) 0x0080, "ug", "ug"), LANG_0081((short) 0x0081, "mi", "mi"), LANG_0082((short) 0x0082, "oc", "oc"),
    LANG_0083((short) 0x0083, "co", "co"), LANG_0084((short) 0x0084, "gsw", "gsw"),
    LANG_0085((short) 0x0085, "sah", "sah"), LANG_0086((short) 0x0086, "qut", "qut"),
    LANG_0087((short) 0x0087, "rw", "rw"), LANG_0088((short) 0x0088, "wo", "wo"), LANG_0089((short) 0x0089, null, null),
    LANG_008A((short) 0x008A, null, null), LANG_008B((short) 0x008B, null, null),
    LANG_008C((short) 0x008C, "prs", "prs"), LANG_008D((short) 0x008D, null, null),
    LANG_008E((short) 0x008E, null, null), LANG_008F((short) 0x008F, null, null), LANG_0090((short) 0x0090, null, null),
    LANG_0091((short) 0x0091, "gd", "gd"), LANG_0092((short) 0x0092, "ku", "ku"),
    LANG_0093((short) 0x0093, "quc", "quc"), LANG_0401((short) 0x0401, "ar_sa", "ar-SA"),
    LANG_0402((short) 0x0402, "bg_bg", "bg-BG"), LANG_0403((short) 0x0403, "ca_es", "ca-ES"),
    LANG_0404((short) 0x0404, "zh_tw", "zh-TW"), LANG_0405((short) 0x0405, "cs_cz", "cs-CZ"),
    LANG_0406((short) 0x0406, "da_dk", "da-DK"), LANG_0407((short) 0x0407, "de_de", "de-DE"),
    LANG_0408((short) 0x0408, "el_gr", "el-GR"), LANG_0409((short) 0x0409, "en_us", "en-US"),
    LANG_040A((short) 0x040A, "es_es_tradnl", "es-ES-tradnl"), LANG_040B((short) 0x040B, "fi_fi", "fi-FI"),
    LANG_040C((short) 0x040C, "fr_fr", "fr-FR"), LANG_040D((short) 0x040D, "he_il", "he-IL"),
    LANG_040E((short) 0x040E, "hu_hu", "hu-HU"), LANG_040F((short) 0x040F, "is_is", "is-IS"),
    LANG_0410((short) 0x0410, "it_it", "it-IT"), LANG_0411((short) 0x0411, "ja_jp", "ja-JP"),
    LANG_0412((short) 0x0412, "ko_kr", "ko-KR"), LANG_0413((short) 0x0413, "nl_nl", "nl-NL"),
    LANG_0414((short) 0x0414, "nb_no", "nb-NO"), LANG_0415((short) 0x0415, "pl_pl", "pl-PL"),
    LANG_0416((short) 0x0416, "pt_br", "pt-BR"), LANG_0417((short) 0x0417, "rm_ch", "rm-CH"),
    LANG_0418((short) 0x0418, "ro_ro", "ro-RO"), LANG_0419((short) 0x0419, "ru_ru", "ru-RU"),
    LANG_041A((short) 0x041A, "hr_hr", "hr-HR"), LANG_041B((short) 0x041B, "sk_sk", "sk-SK"),
    LANG_041C((short) 0x041C, "sq_al", "sq-AL"), LANG_041D((short) 0x041D, "sv_se", "sv-SE"),
    LANG_041E((short) 0x041E, "th_th", "th-TH"), LANG_041F((short) 0x041F, "tr_tr", "tr-TR"),
    LANG_0420((short) 0x0420, "ur_pk", "ur-PK"), LANG_0421((short) 0x0421, "id_id", "id-ID"),
    LANG_0422((short) 0x0422, "uk_ua", "uk-UA"), LANG_0423((short) 0x0423, "be_by", "be-BY"),
    LANG_0424((short) 0x0424, "sl_si", "sl-SI"), LANG_0425((short) 0x0425, "et_ee", "et-EE"),
    LANG_0426((short) 0x0426, "lv_lv", "lv-LV"), LANG_0427((short) 0x0427, "lt_lt", "lt-LT"),
    LANG_0428((short) 0x0428, "tg_cyrl_tj", "tg-Cyrl-TJ"), LANG_0429((short) 0x0429, "fa_ir", "fa-IR"),
    LANG_042A((short) 0x042A, "vi_vn", "vi-VN"), LANG_042B((short) 0x042B, "hy_am", "hy-AM"),
    LANG_042C((short) 0x042C, "az_latn_az", "az-Latn-AZ"), LANG_042D((short) 0x042D, "eu_es", "eu-ES"),
    LANG_042E((short) 0x042E, "hsb_de", "hsb-DE"), LANG_042F((short) 0x042F, "mk_mk", "mk-MK"),
    LANG_0430((short) 0x0430, "st_za", "st-ZA"), LANG_0431((short) 0x0431, "ts_za", "ts-ZA"),
    LANG_0432((short) 0x0432, "tn_za", "tn-ZA"), LANG_0433((short) 0x0433, "ve_za", "ve-ZA"),
    LANG_0434((short) 0x0434, "xh_za", "xh-ZA"), LANG_0435((short) 0x0435, "zu_za", "zu-ZA"),
    LANG_0436((short) 0x0436, "af_za", "af-ZA"), LANG_0437((short) 0x0437, "ka_ge", "ka-GE"),
    LANG_0438((short) 0x0438, "fo_fo", "fo-FO"), LANG_0439((short) 0x0439, "hi_in", "hi-IN"),
    LANG_043A((short) 0x043A, "mt_mt", "mt-MT"), LANG_043B((short) 0x043B, "se_no", "se-NO"),
    LANG_043D((short) 0x043D, "yi_hebr", "yi-Hebr"), LANG_043E((short) 0x043E, "ms_my", "ms-MY"),
    LANG_043F((short) 0x043F, "kk_kz", "kk-KZ"), LANG_0440((short) 0x0440, "ky_kg", "ky-KG"),
    LANG_0441((short) 0x0441, "sw_ke", "sw-KE"), LANG_0442((short) 0x0442, "tk_tm", "tk-TM"),
    LANG_0443((short) 0x0443, "uz_latn_uz", "uz-Latn-UZ"), LANG_0444((short) 0x0444, "tt_ru", "tt-RU"),
    LANG_0445((short) 0x0445, "bn_in", "bn-IN"), LANG_0446((short) 0x0446, "pa_in", "pa-IN"),
    LANG_0447((short) 0x0447, "gu_in", "gu-IN"), LANG_0448((short) 0x0448, "or_in", "or-IN"),
    LANG_0449((short) 0x0449, "ta_in", "ta-IN"), LANG_044A((short) 0x044A, "te_in", "te-IN"),
    LANG_044B((short) 0x044B, "kn_in", "kn-IN"), LANG_044C((short) 0x044C, "ml_in", "ml-IN"),
    LANG_044D((short) 0x044D, "as_in", "as-IN"), LANG_044E((short) 0x044E, "mr_in", "mr-IN"),
    LANG_044F((short) 0x044F, "sa_in", "sa-IN"), LANG_0450((short) 0x0450, "mn_mn", "mn-MN"),
    LANG_0451((short) 0x0451, "bo_cn", "bo-CN"), LANG_0452((short) 0x0452, "cy_gb", "cy-GB"),
    LANG_0453((short) 0x0453, "km_kh", "km-KH"), LANG_0454((short) 0x0454, "lo_la", "lo-LA"),
    LANG_0455((short) 0x0455, "my_mm", "my-MM"), LANG_0456((short) 0x0456, "gl_es", "gl-ES"),
    LANG_0457((short) 0x0457, "kok_in", "kok-IN"), LANG_0458((short) 0x0458, "mni_in", "mni-IN"),
    LANG_0459((short) 0x0459, "sd_deva_in", "sd-Deva-IN"), LANG_045A((short) 0x045A, "syr_sy", "syr-SY"),
    LANG_045B((short) 0x045B, "si_lk", "si-LK"), LANG_045C((short) 0x045C, "chr_cher_us", "chr-Cher-US"),
    LANG_045D((short) 0x045D, "iu_cans_ca", "iu-Cans-CA"), LANG_045E((short) 0x045E, "am_et", "am-ET"),
    LANG_045F((short) 0x045F, "tzm_arab_ma", "tzm-Arab-MA"), LANG_0460((short) 0x0460, "ks_arab", "ks-Arab"),
    LANG_0461((short) 0x0461, "ne_np", "ne-NP"), LANG_0462((short) 0x0462, "fy_nl", "fy-NL"),
    LANG_0463((short) 0x0463, "ps_af", "ps-AF"), LANG_0464((short) 0x0464, "fil_ph", "fil-PH"),
    LANG_0465((short) 0x0465, "dv_mv", "dv-MV"), LANG_0466((short) 0x0466, "bin_ng", "bin-NG"),
    LANG_0467((short) 0x0467, "fuv_ng", "fuv-NG"), LANG_0468((short) 0x0468, "ha_latn_ng", "ha-Latn-NG"),
    LANG_0469((short) 0x0469, "ibb_ng", "ibb-NG"), LANG_046A((short) 0x046A, "yo_ng", "yo-NG"),
    LANG_046B((short) 0x046B, "quz_bo", "quz-BO"), LANG_046C((short) 0x046C, "nso_za", "nso-ZA"),
    LANG_046D((short) 0x046D, "ba_ru", "ba-RU"), LANG_046E((short) 0x046E, "lb_lu", "lb-LU"),
    LANG_046F((short) 0x046F, "kl_gl", "kl-GL"), LANG_0470((short) 0x0470, "ig_ng", "ig-NG"),
    LANG_0471((short) 0x0471, "kr_ng", "kr-NG"), LANG_0472((short) 0x0472, "om_et", "om-ET"),
    LANG_0473((short) 0x0473, "ti_et", "ti-ET"), LANG_0474((short) 0x0474, "gn_py", "gn-PY"),
    LANG_0475((short) 0x0475, "haw_us", "haw-US"), LANG_0476((short) 0x0476, "la_latn", "la-Latn"),
    LANG_0477((short) 0x0477, "so_so", "so-SO"), LANG_0478((short) 0x0478, "ii_cn", "ii-CN"),
    LANG_0479((short) 0x0479, "pap_029", "pap-029"), LANG_047A((short) 0x047A, "arn_cl", "arn-CL"),
    LANG_047C((short) 0x047C, "moh_ca", "moh-CA"), LANG_047E((short) 0x047E, "br_fr", "br-FR"),
    LANG_0480((short) 0x0480, "ug_cn", "ug-CN"), LANG_0481((short) 0x0481, "mi_nz", "mi-NZ"),
    LANG_0482((short) 0x0482, "oc_fr", "oc-FR"), LANG_0483((short) 0x0483, "co_fr", "co-FR"),
    LANG_0484((short) 0x0484, "gsw_fr", "gsw-FR"), LANG_0485((short) 0x0485, "sah_ru", "sah-RU"),
    LANG_0486((short) 0x0486, "qut_gt", "qut-GT"), LANG_0487((short) 0x0487, "rw_rw", "rw-RW"),
    LANG_0488((short) 0x0488, "wo_sn", "wo-SN"), LANG_048C((short) 0x048C, "prs_af", "prs-AF"),
    LANG_048D((short) 0x048D, "plt_mg", "plt-MG"), LANG_048E((short) 0x048E, "zh_yue_hk", "yue-HK"),
    LANG_048F((short) 0x048F, "tdd_tale_cn", "tdd-Tale-CN"), LANG_0490((short) 0x0490, "khb_talu_cn", "khb-Talu-CN"),
    LANG_0491((short) 0x0491, "gd_gb", "gd-GB"), LANG_0492((short) 0x0492, "ku_arab_iq", "ku-Arab-IQ"),
    LANG_0493((short) 0x0493, "quc_co", "quc-CO"), LANG_0501((short) 0x0501, "qps_ploc", "qps-Ploc"),
    LANG_05FE((short) 0x05FE, "qps_ploca", "qps-ploca"), LANG_0801((short) 0x0801, "ar_iq", "ar-IQ"),
    LANG_0803((short) 0x0803, "ca_es_valencia", "ca-ES-valencia"), LANG_0804((short) 0x0804, "zh_cn", "zh-CN"),
    LANG_0807((short) 0x0807, "de_ch", "de-CH"), LANG_0809((short) 0x0809, "en_gb", "en-GB"),
    LANG_080A((short) 0x080A, "es_mx", "es-MX"), LANG_080C((short) 0x080C, "fr_be", "fr-BE"),
    LANG_0810((short) 0x0810, "it_ch", "it-CH"), LANG_0811((short) 0x0811, "ja_ploc_jp", "ja-Ploc-JP"),
    LANG_0813((short) 0x0813, "nl_be", "nl-BE"), LANG_0814((short) 0x0814, "nn_no", "nn-NO"),
    LANG_0816((short) 0x0816, "pt_pt", "pt-PT"), LANG_0818((short) 0x0818, "ro_md", "ro-MD"),
    LANG_0819((short) 0x0819, "ru_md", "ru-MD"), LANG_081A((short) 0x081A, "sr_latn_cs", "sr-Latn-CS"),
    LANG_081D((short) 0x081D, "sv_fi", "sv-FI"), LANG_0820((short) 0x0820, "ur_in", "ur-IN"),
    LANG_0827((short) 0x0827, null, null), LANG_082C((short) 0x082C, "az_cyrl_az", "az-Cyrl-AZ"),
    LANG_082E((short) 0x082E, "dsb_de", "dsb-DE"), LANG_0832((short) 0x0832, "tn_bw", "tn-BW"),
    LANG_083B((short) 0x083B, "se_se", "se-SE"), LANG_083C((short) 0x083C, "ga_ie", "ga-IE"),
    LANG_083E((short) 0x083E, "ms_bn", "ms-BN"), LANG_0843((short) 0x0843, "uz_cyrl_uz", "uz-Cyrl-UZ"),
    LANG_0845((short) 0x0845, "bn_bd", "bn-BD"), LANG_0846((short) 0x0846, "pa_arab_pk", "pa-Arab-PK"),
    LANG_0849((short) 0x0849, "ta_lk", "ta-LK"), LANG_0850((short) 0x0850, "mn_mong_cn", "mn-Mong-CN"),
    LANG_0851((short) 0x0851, "bo_bt", "bo-BT"), LANG_0859((short) 0x0859, "sd_arab_pk", "sd-Arab-PK"),
    LANG_085D((short) 0x085D, "iu_latn_ca", "iu-Latn-CA"), LANG_085F((short) 0x085F, "tzm_latn_dz", "tzm-Latn-DZ"),
    LANG_0860((short) 0x0860, "ks_deva", "ks-Deva"), LANG_0861((short) 0x0861, "ne_in", "ne-IN"),
    LANG_0867((short) 0x0867, "ff_latn_sn", "ff-Latn-SN"), LANG_086B((short) 0x086B, "quz_ec", "quz-EC"),
    LANG_0873((short) 0x0873, "ti_er", "ti-ER"), LANG_09FF((short) 0x09FF, "qps_plocm", "qps-plocm"),
    LANG_0C01((short) 0x0C01, "ar_eg", "ar-EG"), LANG_0C04((short) 0x0C04, "zh_hk", "zh-HK"),
    LANG_0C07((short) 0x0C07, "de_at", "de-AT"), LANG_0C09((short) 0x0C09, "en_au", "en-AU"),
    LANG_0C0A((short) 0x0C0A, "es_es", "es-ES"), LANG_0C0C((short) 0x0C0C, "fr_ca", "fr-CA"),
    LANG_0C1A((short) 0x0C1A, "sr_cyrl_cs", "sr-Cyrl-CS"), LANG_0C3B((short) 0x0C3B, "se_fi", "se-FI"),
    LANG_0C50((short) 0x0C50, "mn_mong_mn", "mn-Mong-MN"), LANG_0C51((short) 0x0C51, "dz_bt", "dz-BT"),
    LANG_0C5F((short) 0x0C5F, "tmz_ma", "tmz-MA"), LANG_0C6b((short) 0x0C6b, "quz_pe", "quz-PE"),
    LANG_1001((short) 0x1001, "ar_ly", "ar-LY"), LANG_1004((short) 0x1004, "zh_sg", "zh-SG"),
    LANG_1007((short) 0x1007, "de_lu", "de-LU"), LANG_1009((short) 0x1009, "en_ca", "en-CA"),
    LANG_100A((short) 0x100A, "es_gt", "es-GT"), LANG_100C((short) 0x100C, "fr_ch", "fr-CH"),
    LANG_101A((short) 0x101A, "hr_ba", "hr-BA"), LANG_103B((short) 0x103B, "smj_no", "smj-NO"),
    LANG_105F((short) 0x105F, "tzm_tfng_ma", "tzm-Tfng-MA"), LANG_1401((short) 0x1401, "ar_dz", "ar-DZ"),
    LANG_1404((short) 0x1404, "zh_mo", "zh-MO"), LANG_1407((short) 0x1407, "de_li", "de-LI"),
    LANG_1409((short) 0x1409, "en_nz", "en-NZ"), LANG_140A((short) 0x140A, "es_cr", "es-CR"),
    LANG_140C((short) 0x140C, "fr_lu", "fr-LU"), LANG_141A((short) 0x141A, "bs_latn_ba", "bs-Latn-BA"),
    LANG_143B((short) 0x143B, "smj_se", "smj-SE"), LANG_1801((short) 0x1801, "ar_ma", "ar-MA"),
    LANG_1809((short) 0x1809, "en_ie", "en-IE"), LANG_180A((short) 0x180A, "es_pa", "es-PA"),
    LANG_180C((short) 0x180C, "fr_mc", "fr-MC"), LANG_181A((short) 0x181A, "sr_latn_ba", "sr-Latn-BA"),
    LANG_183B((short) 0x183B, "sma_no", "sma-NO"), LANG_1C01((short) 0x1C01, "ar_tn", "ar-TN"),
    LANG_1C09((short) 0x1C09, "en_za", "en-ZA"), LANG_1C0A((short) 0x1C0A, "es_do", "es-DO"),
    LANG_1C0C((short) 0x1C0C, null, null), LANG_1C1A((short) 0x1C1A, "sr_cyrl_ba", "sr-Cyrl-BA"),
    LANG_1C3B((short) 0x1C3B, "sma_se", "sma-SE"), LANG_2001((short) 0x2001, "ar_om", "ar-OM"),
    LANG_2008((short) 0x2008, null, null), LANG_2009((short) 0x2009, "en_jm", "en-JM"),
    LANG_200A((short) 0x200A, "es_ve", "es-VE"), LANG_200C((short) 0x200C, "fr_re", "fr-RE"),
    LANG_201A((short) 0x201A, "bs_cyrl_ba", "bs-Cyrl-BA"), LANG_203B((short) 0x203B, "sms_fi", "sms-FI"),
    LANG_2401((short) 0x2401, "ar_ye", "ar-YE"), LANG_2409((short) 0x2409, "en_029", "en-029"),
    LANG_240A((short) 0x240A, "es_co", "es-CO"), LANG_240C((short) 0x240C, "fr_cd", "fr-CD"),
    LANG_241A((short) 0x241A, "sr_latn_rs", "sr-Latn-RS"), LANG_243B((short) 0x243B, "smn_fi", "smn-FI"),
    LANG_2801((short) 0x2801, "ar_sy", "ar-SY"), LANG_2809((short) 0x2809, "en_bz", "en-BZ"),
    LANG_280A((short) 0x280A, "es_pe", "es-PE"), LANG_280C((short) 0x280C, "fr_sn", "fr-SN"),
    LANG_281A((short) 0x281A, "sr_cyrl_rs", "sr-Cyrl-RS"), LANG_2C01((short) 0x2C01, "ar_jo", "ar-JO"),
    LANG_2C09((short) 0x2C09, "en_tt", "en-TT"), LANG_2C0A((short) 0x2C0A, "es_ar", "es-AR"),
    LANG_2C0C((short) 0x2C0C, "fr_cm", "fr-CM"), LANG_2C1A((short) 0x2C1A, "sr_latn_me", "sr-Latn-ME"),
    LANG_3001((short) 0x3001, "ar_lb", "ar-LB"), LANG_3009((short) 0x3009, "en_zw", "en-ZW"),
    LANG_300A((short) 0x300A, "es_ec", "es-EC"), LANG_300C((short) 0x300C, "fr_ci", "fr-CI"),
    LANG_301A((short) 0x301A, "sr_cyrl_me", "sr-Cyrl-ME"), LANG_3401((short) 0x3401, "ar_kw", "ar-KW"),
    LANG_3409((short) 0x3409, "en_ph", "en-PH"), LANG_340A((short) 0x340A, "es_cl", "es-CL"),
    LANG_340C((short) 0x340C, "fr_ml", "fr-ML"), LANG_3801((short) 0x3801, "ar_ae", "ar-AE"),
    LANG_3809((short) 0x3809, "en_id", "en-ID"), LANG_380A((short) 0x380A, "es_uy", "es-UY"),
    LANG_380C((short) 0x380C, "fr_ma", "fr-MA"), LANG_3c01((short) 0x3c01, "ar_bh", "ar-BH"),
    LANG_3c09((short) 0x3c09, "en_hk", "en-HK"), LANG_3c0A((short) 0x3c0A, "es_py", "es-PY"),
    LANG_3c0C((short) 0x3c0C, "fr_ht", "fr-HT"), LANG_4001((short) 0x4001, "ar_qa", "ar-QA"),
    LANG_4009((short) 0x4009, "en_in", "en-IN"), LANG_400A((short) 0x400A, "es_bo", "es-BO"),
    LANG_4401((short) 0x4401, "ar_ploc_sa", "ar-Ploc-SA"), LANG_4409((short) 0x4409, "en_my", "en-MY"),
    LANG_440A((short) 0x440A, "es_sv", "es-SV"), LANG_4801((short) 0x4801, "ar_145", "ar-145"),
    LANG_4809((short) 0x4809, "en_sg", "en-SG"), LANG_480A((short) 0x480A, "es_hn", "es-HN"),
    LANG_4C09((short) 0x4C09, "en_ae", "en-AE"), LANG_4C0A((short) 0x4C0A, "es_ni", "es-NI"),
    LANG_5009((short) 0x5009, "en_bh", "en-BH"), LANG_500A((short) 0x500A, "es_pr", "es-PR"),
    LANG_5409((short) 0x5409, "en_eg", "en-EG"), LANG_540A((short) 0x540A, "es_us", "es-US"),
    LANG_5809((short) 0x5809, "en_jo", "en-JO"), LANG_580A((short) 0x580A, "es_419", "es-419"),
    LANG_5C09((short) 0x5C09, "en_kw", "en-KW"), LANG_5C0A((short) 0x5C0A, "es_cu", "es-CU"),
    LANG_6009((short) 0x6009, "en_tr", "en-TR"), LANG_6409((short) 0x6409, "en_ye", "en-YE"),
    LANG_641A((short) 0x641A, "bs_cyrl", "bs-Cyrl"), LANG_681A((short) 0x681A, "bs_latn", "bs-Latn"),
    LANG_6C1A((short) 0x6C1A, "sr_cyrl", "sr-Cyrl"), LANG_701A((short) 0x701A, "sr_latn", "sr-Latn"),
    LANG_703B((short) 0x703B, "smn", "smn"), LANG_742C((short) 0x742C, "az_cyrl", "az-Cyrl"),
    LANG_743B((short) 0x743B, "sms", "sms"), LANG_7804((short) 0x7804, "zh", "zh"),
    LANG_7814((short) 0x7814, "nn", "nn"), LANG_781A((short) 0x781A, "bs", "bs"),
    LANG_782C((short) 0x782C, "az_latn", "az-Latn"), LANG_783B((short) 0x783B, "sma", "sma"),
    LANG_7843((short) 0x7843, "uz_cyrl", "uz-Cyrl"), LANG_7850((short) 0x7850, "mn_cyrl", "mn-Cyrl"),
    LANG_785D((short) 0x785D, "iu_cans", "iu-Cans"), LANG_785F((short) 0x785F, "tzm_tfng", "tzm-Tfng"),
    LANG_7C04((short) 0x7C04, "zh_hant", "zh-Hant"), LANG_7C14((short) 0x7C14, "nb", "nb"),
    LANG_7C1A((short) 0x7C1A, "sr", "sr"), LANG_7C28((short) 0x7C28, "tg_cyrl", "tg-Cyrl"),
    LANG_7C2E((short) 0x7C2E, "dsb", "dsb"), LANG_7C3B((short) 0x7C3B, "smj", "smj"),
    LANG_7C43((short) 0x7C43, "uz_latn", "uz-Latn"), LANG_7C46((short) 0x7C46, "pa_arab", "pa-Arab"),
    LANG_7C50((short) 0x7C50, "mn_mong", "mn-Mong"), LANG_7C59((short) 0x7C59, "sd_arab", "sd-Arab"),
    LANG_7C5C((short) 0x7C5C, "chr_cher", "chr-Cher"), LANG_7C5D((short) 0x7C5D, "iu_latn", "iu-Latn"),
    LANG_7C5F((short) 0x7C5F, "tzm_latn", "tzm-Latn"), LANG_7C67((short) 0x7C67, "ff_latn", "ff-Latn"),
    LANG_7C68((short) 0x7C68, "ha_latn", "ha-Latn"), LANG_7C92((short) 0x7C92, "ku_arab", "ku-Arab"),
    LANG_F2EE((short) 0xF2EE, null, null), LANG_E40C((short) 0xE40C, "fr-015", null),
    LANG_EEEE((short) 0xEEEE, null, null);

    private final short languageID;
    private final String msId;
    private final String languageTag;

    LanguageID(short languageID, String msId, String languageTag) {
        this.languageID = languageID;
        this.msId = msId;
        this.languageTag = languageTag;
    }

    public String getMsId() {
        return msId;
    }

    public Optional<String> getLanguageTag() {
        return Optional.ofNullable(languageTag);
    }

    public Optional<Locale> getLocale() {
        return getLanguageTag().map(Locale::forLanguageTag);
    }

    private static final Map<String, LanguageID> languageTagLookup = Stream.of(values())
            .filter(lId -> lId.getLanguageTag()
                    .isPresent())
            .collect(Collectors.toMap(lId -> lId.getLanguageTag()
                    .get(), Function.identity()));

    private static final Map<Short, LanguageID> lcidLookup = Stream.of(values())
            .collect(Collectors.toMap(LanguageID::getLanguageID, Function.identity()));

    public static LanguageID lookupByLanguageTag(String languageTag) {
        return languageTagLookup.get(languageTag);
    }

    public static LanguageID lookupByLcid(short languageID) {
        return lcidLookup.get(languageID);
    }

    public short getLanguageID() {
        return languageID;
    }
}