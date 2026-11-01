package sv.edu.utec.app;

import sv.edu.utec.modelo.Producto;
import java.util.List;
public final class DatosDemo {
    private DatosDemo() { }
    public static List<Producto> productos() {
        return List.of(
                new Producto("P-001", "Cafe molido 400 g", "Abarrotes", 4.75, 120),
                new Producto("P-002", "Frijol rojo 1 lb", "Abarrotes", 1.35, 300),
                new Producto("P-003", "Arroz blanco 2 lb", "Abarrotes", 1.60, 250),
                new Producto("P-004", "Aceite vegetal 750 ml", "Abarrotes", 2.95, 90),
                new Producto("P-005", "Detergente 1 kg", "Limpieza", 3.10, 80),
                new Producto("P-006", "Lejia 1 galon", "Limpieza", 2.25, 60),
                new Producto("P-007", "Agua purificada 600 ml", "Bebidas", 0.50, 400),
                new Producto("P-008", "Jugo de naranja 1 L", "Bebidas", 1.95, 75));
    }
}