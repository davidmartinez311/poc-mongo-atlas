package sv.edu.utec.modelo;

public class Producto {

    private String codigo;
    private String nombre;
    private String categoria;
    private double precio;
    private int existencia;
    public Producto() { }
    public Producto(String codigo, String nombre, String categoria,
                    double precio, int existencia) {
        setCodigo(codigo);
        setNombre(nombre);
        setCategoria(categoria);
        setPrecio(precio);
        setExistencia(existencia);
    }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo es obligatorio");
        }
        this.codigo = codigo.trim().toUpperCase();
    }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.nombre = nombre.trim();
    }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }
        this.categoria = categoria.trim();
    }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) {
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.precio = precio;
    }
    public int getExistencia() { return existencia; }
    public void setExistencia(int existencia) {
        if (existencia < 0) {
            throw new IllegalArgumentException("La existencia no puede ser negativa");
        }
        this.existencia = existencia;
    }
    @Override
    public String toString() {
        return String.format("%-6s %-24s %-10s $%7.2f %4d u.",
                codigo, nombre, categoria, precio, existencia);
    }
}