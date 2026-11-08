package sv.edu.utec.app;

import sv.edu.utec.datos.ConexionFirestore;
import sv.edu.utec.datos.FirestoreProductoRepositorio;
import sv.edu.utec.datos.ProductoRepositorio;
import sv.edu.utec.datos.RepositorioException;
import sv.edu.utec.modelo.Producto;

import java.util.List;

/** Comprueba, parte por parte, lo que cambia al pasar de MongoDB a Firestore. */
public class PruebaFirestore {

    public static void main(String[] args) {
        try {
            ProductoRepositorio repo =
                    new FirestoreProductoRepositorio(ConexionFirestore.getFirestore());

            // ---- PARTE C: CREATE, DELETE y buscar uno ----
            // 0. Limpieza: la prueba se puede ejecutar varias veces
            for (Producto p : DatosDemo.productos()) {
                repo.eliminar(p.getCodigo());
            }
            repo.eliminar("P-009");
            repo.eliminar("P-011");
            System.out.println("0) Limpieza lista");

            // 1. CREATE: el codigo es el ID del documento
            int n = repo.insertarVarios(DatosDemo.productos());
            System.out.println("1) Carga inicial: " + n + " productos");
            repo.insertar(new Producto("P-009", "Azucar blanca 1 kg",
                    "Abarrotes", 1.10, 150));
            System.out.println("   Insertado: P-009");
            try {
                repo.insertar(new Producto("P-001", "Duplicado", "Abarrotes", 1.00, 1));
            } catch (RepositorioException e) {
                System.out.println("   Rechazado: " + e.getMessage());
            }
            try {                                   // lote: todo o nada
                repo.insertarVarios(List.of(
                        new Producto("P-011", "Sal de mesa 1 kg", "Abarrotes", 0.85, 200),
                        new Producto("P-002", "Duplicado", "Abarrotes", 1.00, 1)));
            } catch (RepositorioException e) {
                System.out.println("   Lote rechazado: " + e.getMessage());
            }
            System.out.println("   P-011 existe despues del lote: "
                    + repo.buscarPorCodigo("P-011").isPresent());
            try {                                   // inyeccion de ruta
                repo.buscarPorCodigo("P-001/ventas/V-1");
            } catch (IllegalArgumentException e) {
                System.out.println("   Rechazado por Java: " + e.getMessage());
            }

            // 2. DELETE
            System.out.println("2) P-009 eliminado: " + repo.eliminar("P-009"));
            System.out.println("   P-009 eliminado otra vez: " + repo.eliminar("P-009"));

            // ---- PARTE D: READ ----
            // 3. Consultas
            System.out.println("3) Buscar P-004: " + repo.buscarPorCodigo("P-004")
                    .map(Producto::toString).orElse("No existe"));
            System.out.println("   Total de productos: " + repo.listarTodos().size());
            System.out.println("   Precio entre $1.00 y $2.00:");
            repo.buscarPorRangoDePrecio(1.00, 2.00)
                    .forEach(p -> System.out.println("    " + p));
            System.out.println("   Nombre contiene \"LB\":");
            repo.buscarPorNombre("LB")
                    .forEach(p -> System.out.println("    " + p));
            System.out.println("   Nombre contiene \".*\": "
                    + repo.buscarPorNombre(".*").size() + " resultados");
            System.out.println("   Categoria Limpieza:");      // requiere indice compuesto
            repo.listarPorCategoria("Limpieza")
                    .forEach(p -> System.out.println("    " + p));

            // ---- PARTE E: UPDATE y REPORTE ----
            // 4. Actualizar y mover existencias (transacciones)
            Producto frijol = repo.buscarPorCodigo("P-002").orElseThrow();
            frijol.setPrecio(1.45);
            System.out.println("4) P-002 actualizado: " + repo.actualizar(frijol));
            System.out.println("   P-404 actualizado: " + repo.actualizar(
                    new Producto("P-404", "No existe", "Abarrotes", 1.00, 1)));
            System.out.println("   Entrada de 40 a P-005: " + repo.ajustarExistencia("P-005", 40));
            System.out.println("   Salida de 500 de P-005: " + repo.ajustarExistencia("P-005", -500));
            System.out.println("   Salida de 20 de P-005: " + repo.ajustarExistencia("P-005", -20));
            // 5. Reporte agrupado en Java
            System.out.println("5) Resumen por categoria:");
            repo.resumenPorCategoria().forEach(r -> System.out.printf(
                    "   %-10s %d productos %4d u. $%9.2f%n",
                    r.categoria(), r.productos(), r.unidades(), r.valorInventario()));

        } catch (RepositorioException | IllegalStateException e) {
            System.err.println("Error de datos: " + e.getMessage());
        } finally {
            ConexionFirestore.cerrar();
        }
    }
}