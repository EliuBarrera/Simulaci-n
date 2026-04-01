    package com.usta.utils;

    /**
     * Transforma coordenadas entre el sistema de pantalla (px)
     * y el sistema matemático estándar (origen abajo-izquierda, Y crece arriba).
     *
     * El canvas usa:
     *   - Origen (0,0) en la esquina superior-izquierda
     *   - Y crece hacia abajo
     *   - Margen de 40px en los bordes
     *   - Escala: 100 px = 1 unidad de distancia
     *
     * El sistema matemático usa:
     *   - Origen (0,0) en la esquina inferior-izquierda del área útil
     *   - Y crece hacia arriba
     *   - Unidades de distancia reales (m, cm, nm, etc.)
     *
     * En modo 3D isométrico:
     *   - Z es una coordenada lógica (no tiene representación directa en px)
     *   - Se proyecta a 2D usando proyección isométrica
     */
    public class CoordenadasTransformador {

        private static final double MARGIN      = 40.0;  // margen del canvas en px
        private static final double PX_POR_UNIT = 100.0; // 100 px = 1 unidad de distancia

        // Ángulos de proyección isométrica (en radianes)
        private static final double ISO_ANGLE = Math.toRadians(30); // 30 grados
        private static final double COS_ISO   = Math.cos(ISO_ANGLE);
        private static final double SIN_ISO   = Math.sin(ISO_ANGLE);

        private final double canvasHeight;          // altura total del canvas en px
        private final UnidadDistancia unidad;       // unidad de distancia activa

        public CoordenadasTransformador(double canvasHeight, UnidadDistancia unidad) {
            this.canvasHeight = canvasHeight;
            this.unidad       = unidad;
        }

        // ── De pantalla (px) a sistema matemático (unidades reales) ──────────────

        /**
         * Convierte una coordenada X de pantalla a unidades de distancia reales.
         * El eje X no se invierte: solo se descuenta el margen y se escala.
         */
        public double pxXToUnidad(double pxX) {
            return (pxX - MARGIN) / PX_POR_UNIT;
        }

        /**
         * Convierte una coordenada Y de pantalla a unidades de distancia reales.
         * Se invierte el eje Y: en pantalla Y crece hacia abajo,
         * en el sistema matemático Y crece hacia arriba.
         * El origen matemático (0,0) coincide con la esquina inferior-izquierda
         * del área útil del canvas.
         */
        public double pxYToUnidad(double pxY) {
            double alturaUtil = canvasHeight - MARGIN; // px desde el fondo del área útil
            return (alturaUtil - pxY) / PX_POR_UNIT;
        }

        /**
         * Convierte una coordenada Z lógica a unidades de distancia reales.
         * Z no tiene inversión — es una coordenada lógica directa.
         * El valor Z se almacena directamente en unidades del plano.
         */
        public double zToUnidad(double z) {
            return z;  // Z ya está en unidades del plano
        }

        // ── De sistema matemático (unidades reales) a pantalla (px) ──────────────

        /** Convierte X en unidades reales a px de pantalla. */
        public double unidadXToPx(double ux) {
            return MARGIN + ux * PX_POR_UNIT;
        }

        /** Convierte Y en unidades reales a px de pantalla (invierte el eje). */
        public double unidadYToPx(double uy) {
            double alturaUtil = canvasHeight - MARGIN;
            return alturaUtil - uy * PX_POR_UNIT;
        }

        // ── Proyección isométrica 3D → 2D ───────────────────────────────────────

        /**
         * Proyecta coordenadas lógicas (unidades del plano) a posición X en pantalla (px)
         * usando proyección isométrica.
         * X va a la derecha, Z va oblicuo a la izquierda.
         */
        public double isoXToPx(double ux, double uy, double uz) {
            // En isométrica: screenX = originX + (x * cos30 - z * cos30) * scale
            double originX = MARGIN + 5 * PX_POR_UNIT * COS_ISO; // desplazar para dar espacio al eje Z
            return originX + (ux * COS_ISO - uz * COS_ISO) * PX_POR_UNIT;
        }

        /**
         * Proyecta coordenadas lógicas (unidades del plano) a posición Y en pantalla (px)
         * usando proyección isométrica.
         * Y va hacia arriba, X y Z van oblicuos hacia abajo.
         */
        public double isoYToPx(double ux, double uy, double uz) {
            double alturaUtil = canvasHeight - MARGIN;
            // En isométrica: screenY = originY - (y * scale) - (x * sin30 + z * sin30) * scale
            double originY = alturaUtil - 0.5 * PX_POR_UNIT; // un poco de espacio abajo
            return originY - uy * PX_POR_UNIT - (ux * SIN_ISO + uz * SIN_ISO) * PX_POR_UNIT;
        }

        // ── Conversión de unidades a metros ──────────────────────────────────────

        /** Convierte un valor en la unidad activa a metros. */
        public double toMetros(double valorEnUnidad) {
            return unidad.convertirAMetros(valorEnUnidad);
        }

        // ── Diferencias vectoriales en sistema matemático ─────────────────────────

        /**
         * Calcula Dx entre dos puntos en px, ya en unidades reales.
         * No requiere inversión porque X no se invierte.
         */
        public double dxUnidades(double pxX1, double pxX2) {
            return pxXToUnidad(pxX2) - pxXToUnidad(pxX1);
        }

        /**
         * Calcula Dy entre dos puntos en px, ya en unidades reales y con Y invertido.
         * El resultado es positivo cuando el destino está "más arriba" visualmente.
         */
        public double dyUnidades(double pxY1, double pxY2) {
            return pxYToUnidad(pxY2) - pxYToUnidad(pxY1);
        }

        // ── Getters ───────────────────────────────────────────────────────────────

        public UnidadDistancia getUnidad() {
            return unidad;
        }

        public double getCanvasHeight() {
            return canvasHeight;
        }

        public double getMargin() {
            return MARGIN;
        }

        public double getPxPorUnidad() {
            return PX_POR_UNIT;
        }

        public static double getIsoAngle() {
            return ISO_ANGLE;
        }

        public static double getCosIso() {
            return COS_ISO;
        }

        public static double getSinIso() {
            return SIN_ISO;
        }
    }