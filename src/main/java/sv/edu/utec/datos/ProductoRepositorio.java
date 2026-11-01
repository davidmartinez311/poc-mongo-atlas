package sv.edu.utec.datos;

import sv.edu.utec.modelo.Producto;
import sv.edu.utec.modelo.ResumenCategoria;
import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio {
    // ---- C: Create ----
    void insertar(Producto producto);
    int insertarVarios(List<Producto> productos);
    Optional<Producto> buscarPorCodigo(String codigo);
    List<Producto> listarTodos();
    List<Producto> listarPorCategoria(String categoria);
    List<Producto> buscarPorNombre(String texto);
    List<Producto> buscarPorRangoDePrecio(double minimo, double maximo);
    // ---- U: Update ----
    boolean actualizar(Producto producto);
    boolean ajustarExistencia(String codigo, int cantidad);
    // ---- D: Delete ----
    boolean eliminar(String codigo);
    // ---- Reporte ----
    List<ResumenCategoria> resumenPorCategoria();
}