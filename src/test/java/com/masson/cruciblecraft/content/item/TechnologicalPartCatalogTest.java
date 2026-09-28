package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class TechnologicalPartCatalogTest {
    @Test
    void catalogCoversCircuitAndTechnologicalPartPaths() {
        Set<String> paths = TechnologicalPartCatalog.parts().stream()
                .map(TechnologicalPartCatalog.Part::registryPath)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(TechnologicalPartCatalog.parts().size(), paths.size());
        assertTrue(paths.containsAll(Set.of(
                "compact_electric_motor_ulv",
                "compact_electric_motor_mv",
                "compact_electric_motor_hv",
                "compact_electric_motor_iv",
                "compact_electric_piston_ulv",
                "compact_electric_piston_mv",
                "compact_electric_piston_hv",
                "compact_electric_piston_iv",
                "compact_electric_conveyor_ulv",
                "compact_electric_conveyor_mv",
                "compact_electric_conveyor_hv",
                "compact_electric_conveyor_iv",
                "compact_signal_emitter_ulv",
                "compact_signal_emitter_lv",
                "compact_signal_emitter_mv",
                "compact_signal_emitter_hv",
                "compact_signal_emitter_ev",
                "compact_signal_emitter_iv",
                "compact_sensor_ulv",
                "compact_sensor_lv",
                "compact_sensor_mv",
                "compact_sensor_hv",
                "compact_sensor_ev",
                "compact_sensor_iv",
                "compact_electric_pump_ulv",
                "compact_electric_pump_lv",
                "compact_electric_pump_mv",
                "compact_electric_pump_hv",
                "compact_electric_pump_ev",
                "compact_electric_pump_iv",
                "compact_electric_robot_arm_ulv",
                "compact_electric_robot_arm_lv",
                "compact_electric_robot_arm_mv",
                "compact_electric_robot_arm_hv",
                "compact_electric_robot_arm_ev",
                "compact_electric_robot_arm_iv",
                "compact_force_field_emitter_ulv",
                "compact_force_field_emitter_lv",
                "compact_force_field_emitter_mv",
                "compact_force_field_emitter_hv",
                "compact_force_field_emitter_ev",
                "compact_force_field_emitter_iv",
                "circuit_basic",
                "circuit_good",
                "circuit_advanced",
                "circuit_elite",
                "circuit_master",
                "circuit_ultimate",
                "circuit_wire_gold",
                "circuit_plate_gold",
                "circuit_part_good",
                "circuit_part_advanced",
                "circuit_part_elite",
                "circuit_part_ultimate",
                "circuit_quantum",
                "compact_electric_motor_omega",
                "laser_gas_empty",
                "laser_gas_he",
                "laser_gas_ne",
                "laser_gas_hene",
                "laser_gas_ar",
                "laser_gas_kr",
                "laser_gas_xe",
                "laser_gas_co",
                "laser_gas_co2",
                "processor_crystal_empty",
                "processor_crystal_sapphire")));
    }
}
