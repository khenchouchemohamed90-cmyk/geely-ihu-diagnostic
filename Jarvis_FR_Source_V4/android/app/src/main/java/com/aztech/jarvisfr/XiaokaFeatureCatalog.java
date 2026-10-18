package com.aztech.jarvisfr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Catalogue statique construit depuis Xiaoka547.zip de l'utilisateur :
 * GKAICarControl/oat/arm64/GKAICarControl.vdex (firmware IHU007B 547).
 * Une chaîne *_success est une ressource du fabricant, PAS la preuve d'une action.
 * AUCUN ID de contrôle physique n'est déduit de ces chaînes.
 */
public final class XiaokaFeatureCatalog {
    private XiaokaFeatureCatalog() { }

    public static final class Feature {
        public final String family, name, evidence, permissionHint;
        Feature(String family, String name, String evidence, String permissionHint) {
            this.family = family;
            this.name = name;
            this.evidence = evidence;
            this.permissionHint = permissionHint;
        }
    }
    public static final String SOURCE =
            "Xiaoka547/GKAICarControl/oat/arm64/GKAICarControl.vdex";
    public static final String HVAC_PERMISSION = "android.car.permission.CONTROL_CAR_CLIMATE";
    public static final String DOORS_PERMISSION = "android.car.permission.CONTROL_CAR_DOORS";
    public static final String SEATS_PERMISSION = "android.car.permission.CONTROL_CAR_SEATS";
    public static final String EXTENSION_PERMISSION = "android.car.permission.CAR_VENDOR_EXTENSION";

    private static Feature f(String family, String name, String evidence, String permission) {
        return new Feature(family, name, evidence, permission);
    }

    private static final List<Feature> ITEMS = Collections.unmodifiableList(Arrays.asList(
        f("Climatisation", "Activation et arrêt A/C",
                "ss11_acv2_logic_compressor_open_result_success", HVAC_PERMISSION),
        f("Climatisation", "Activation et arrêt HVAC",
                "ss11_acv2_logic_open_hvac_result", HVAC_PERMISSION),
        f("Climatisation", "Température de consigne",
                "ss11_acv2_logic_set_area_temp_result", HVAC_PERMISSION),
        f("Climatisation", "Ventilation augmenter/diminuer",
                "ss11_acv2_logic_blowing_level_increase_result", HVAC_PERMISSION),
        f("Climatisation", "Distribution visage/pieds",
                "ss11_acv2_logic_blowing_loop_face_leg_result_success", HVAC_PERMISSION),
        f("Climatisation", "Recyclage d'air intérieur",
                "ss11_acv2_logic_loop_mode_inside_open_result_success", HVAC_PERMISSION),
        f("Climatisation", "Air extérieur",
                "ss11_acv2_logic_loop_mode_outside_open_result_success", HVAC_PERMISSION),
        f("Climatisation", "Dégivrage avant",
                "ss11_acv2_logic_defrost_open_result_success", HVAC_PERMISSION),
        f("Climatisation", "Dégivrage arrière",
                "ss11_acv2_logic_defrost_rear_open_result_success", HVAC_PERMISSION),
        f("Vitres", "Vitre avant gauche",
                "ss11_vehiclev2_logic_window_control_row_1_left_open_success", EXTENSION_PERMISSION),
        f("Vitres", "Vitre avant droite",
                "ss11_vehiclev2_logic_window_control_row_1_right_open_success", EXTENSION_PERMISSION),
        f("Vitres", "Vitre arrière gauche",
                "ss11_vehiclev2_logic_window_control_row_2_left_open_success", EXTENSION_PERMISSION),
        f("Vitres", "Vitre arrière droite",
                "ss11_vehiclev2_logic_window_control_row_2_right_open_success", EXTENSION_PERMISSION),
        f("Vitres", "Toutes les vitres",
                "SS11 controlCarAllWindows result", EXTENSION_PERMISSION),
        f("Vitres", "Vitres avant groupées",
                "SS11 controlCarFrontWindows result", EXTENSION_PERMISSION),
        f("Vitres", "Vitres arrière groupées",
                "SS11 controlCarBackWindows result", EXTENSION_PERMISSION),
        f("Toit ouvrant", "Ouverture du toit",
                "ss11_vehiclev2_logic_window_roof_top_open_success", EXTENSION_PERMISSION),
        f("Toit ouvrant", "Fermeture du toit",
                "ss11_vehiclev2_logic_window_roof_top_close_success", EXTENSION_PERMISSION),
        f("Toit ouvrant", "Arrêt du toit",
                "ss11_vehiclev2_logic_window_roof_top_stop_success", EXTENSION_PERMISSION),
        f("Toit ouvrant", "Inclinaison du toit",
                "ss11_vehiclev2_logic_window_roof_top_tilt_success", EXTENSION_PERMISSION),
        f("Coffre", "Ouverture du coffre",
                "ss11_vehiclev2_logic_trunk_open_success", DOORS_PERMISSION),
        f("Coffre", "Fermeture du coffre",
                "ss11_vehiclev2_logic_trunk_close_success", DOORS_PERMISSION),
        f("Sièges", "Chauffage de siège",
                "ss11_acv2_logic_seat_heating_row_1_left_open_success", SEATS_PERMISSION),
        f("Sièges", "Ventilation de siège",
                "ss11_acv2_logic_seat_ventilation_row_1_right_open_success", SEATS_PERMISSION),
        f("Éclairage", "Plafonnier",
                "ss11_vehiclev2_logic_reading_lamp_open_success", EXTENSION_PERMISSION),
        f("Éclairage", "Ambiance lumineuse",
                "ss11_vehiclev2_logic_atmosphere_lamp_open_success", EXTENSION_PERMISSION),
        f("Éclairage", "Couleur ambiance lumineuse",
                "ss11_vehiclev2_logic_set_atmosphere_lamp_color_success", EXTENSION_PERMISSION),
        f("Confort", "Volant chauffant",
                "ss11_vehiclev2_logic_wheel_heating_open_success", EXTENSION_PERMISSION),
        f("Confort", "Affichage tête haute",
                "ss11_vehiclev2_logic_hud_open_success", EXTENSION_PERMISSION),
        f("Confort", "Diffuseur de parfum",
                "ss11_vehiclev2_logic_fragrance_open_success", EXTENSION_PERMISSION),
        f("Essuie-glaces", "Essuie-glace avant",
                "ss11_vehiclev2_logic_front_wiper_open_success", EXTENSION_PERMISSION),
        f("Essuie-glaces", "Essuie-glace arrière",
                "ss11_vehiclev2_logic_rear_wiper_open_success", EXTENSION_PERMISSION)
    ));

    public static List<Feature> entries() { return ITEMS; }
}
