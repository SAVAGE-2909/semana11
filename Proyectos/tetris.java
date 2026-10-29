import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class tetris extends JPanel {
	private static final int ROWS = 20;
	private static final int COLS = 10;
	private static final int CELL = 30;
	private static final int[][] SHAPES = {
		{0x0F00, 0x2222, 0x00F0, 0x4444}, // I
		{0x8E00, 0x6440, 0x0E20, 0x44C0}, // T
		{0x6C00, 0x4620, 0x06C0, 0x8C40}, // S
		{0xC600, 0x2640, 0x0C60, 0x4C80}, // Z
		{0x8C80, 0xE400, 0x4C40, 0x0E40}, // J
		{0x2E00, 0x44C0, 0x0E80, 0xC440}, // L
		{0x6600, 0x6600, 0x6600, 0x6600}  // O
	};
	private static final Color[] COLORS = {
		new Color(40, 220, 230), new Color(170, 70, 220),
		new Color(70, 210, 90), new Color(230, 60, 70),
		new Color(60, 100, 230), new Color(240, 150, 40),
		new Color(240, 220, 50)
	};

	private final int[][] board = new int[ROWS][COLS];
	private final Random random = new Random();
	private final Timer timer;
	private int piece, rotation, pieceX, pieceY;
	private int score, lines, level = 1;
	private boolean paused, gameOver;

	public tetris() {
		setPreferredSize(new Dimension(COLS * CELL + 180, ROWS * CELL));
		setBackground(new Color(18, 20, 30));
		setFocusable(true);
		setupKeys();
		timer = new Timer(600, e -> tick());
		newGame();
		timer.start();
	}

	private void setupKeys() {
		bind("LEFT", "left", () -> move(-1));
		bind("RIGHT", "right", () -> move(1));
		bind("DOWN", "down", () -> { if (!paused && !gameOver && canPlace(pieceX, pieceY + 1, rotation)) { pieceY++; score++; repaint(); } });
		bind("UP", "rotate", this::rotatePiece);
		bind("X", "rotateX", this::rotatePiece);
		bind("SPACE", "drop", this::hardDrop);
		bind("P", "pause", () -> { if (!gameOver) { paused = !paused; repaint(); } });
		bind("R", "restart", this::newGame);
	}

	private void bind(String key, String name, Runnable action) {
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
		getActionMap().put(name, new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) { action.run(); }
		});
	}

	private void newGame() {
		for (int[] row : board) java.util.Arrays.fill(row, 0);
		score = 0;
		lines = 0;
		level = 1;
		paused = false;
		gameOver = false;
		timer.setDelay(600);
		spawnPiece();
		repaint();
	}

	private void spawnPiece() {
		piece = random.nextInt(SHAPES.length);
		rotation = 0;
		pieceX = 3;
		pieceY = 0;
		if (!canPlace(pieceX, pieceY, rotation)) gameOver = true;
		repaint();
	}

	private boolean occupied(int type, int turn, int x, int y) {
		int bits = SHAPES[type][turn];
		return (bits & (1 << (15 - (y * 4 + x)))) != 0;
	}

	private boolean canPlace(int x, int y, int turn) {
		for (int py = 0; py < 4; py++) {
			for (int px = 0; px < 4; px++) {
				if (!occupied(piece, turn, px, py)) continue;
				int bx = x + px, by = y + py;
				if (bx < 0 || bx >= COLS || by >= ROWS) return false;
				if (by >= 0 && board[by][bx] != 0) return false;
			}
		}
		return true;
	}

	private void move(int dx) {
		if (!paused && !gameOver && canPlace(pieceX + dx, pieceY, rotation)) {
			pieceX += dx;
			repaint();
		}
	}

	private void rotatePiece() {
		if (paused || gameOver) return;
		int next = (rotation + 1) % 4;
		int[] kicks = {0, -1, 1, -2, 2};
		for (int kick : kicks) {
			if (canPlace(pieceX + kick, pieceY, next)) {
				pieceX += kick;
				rotation = next;
				repaint();
				return;
			}
		}
	}

	private void hardDrop() {
		if (paused || gameOver) return;
		int distance = 0;
		while (canPlace(pieceX, pieceY + 1, rotation)) { pieceY++; distance++; }
		score += distance * 2;
		lockPiece();
	}

	private void tick() {
		if (paused || gameOver) return;
		if (canPlace(pieceX, pieceY + 1, rotation)) pieceY++;
		else lockPiece();
		repaint();
	}

	private void lockPiece() {
		for (int py = 0; py < 4; py++) {
			for (int px = 0; px < 4; px++) {
				if (occupied(piece, rotation, px, py)) {
					int bx = pieceX + px, by = pieceY + py;
					if (by >= 0) board[by][bx] = piece + 1;
				}
			}
		}
		clearLines();
		spawnPiece();
	}

	private void clearLines() {
		int cleared = 0;
		for (int y = ROWS - 1; y >= 0; y--) {
			boolean full = true;
			for (int x = 0; x < COLS; x++) if (board[y][x] == 0) { full = false; break; }
			if (full) {
				for (int row = y; row > 0; row--) System.arraycopy(board[row - 1], 0, board[row], 0, COLS);
				java.util.Arrays.fill(board[0], 0);
				cleared++;
				y++;
			}
		}
		if (cleared > 0) {
			lines += cleared;
			score += new int[]{0, 100, 300, 500, 800}[cleared] * level;
			level = lines / 10 + 1;
			timer.setDelay(Math.max(80, 600 - (level - 1) * 45));
		}
	}

	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(9, 11, 18));
		g.fillRect(0, 0, COLS * CELL, ROWS * CELL);
		for (int y = 0; y < ROWS; y++) {
			for (int x = 0; x < COLS; x++) {
				if (board[y][x] != 0) drawCell(g, x, y, COLORS[board[y][x] - 1]);
				else {
					g.setColor(new Color(35, 38, 49));
					g.drawRect(x * CELL, y * CELL, CELL, CELL);
				}
			}
		}
		if (!gameOver) {
			int ghostY = pieceY;
			while (canPlace(pieceX, ghostY + 1, rotation)) ghostY++;
			for (int py = 0; py < 4; py++) for (int px = 0; px < 4; px++) {
				if (occupied(piece, rotation, px, py) && ghostY + py >= 0) {
					g.setColor(new Color(COLORS[piece].getRed(), COLORS[piece].getGreen(), COLORS[piece].getBlue(), 65));
					g.fillRect((pieceX + px) * CELL + 2, (ghostY + py) * CELL + 2, CELL - 4, CELL - 4);
				}
			}
			for (int py = 0; py < 4; py++) for (int px = 0; px < 4; px++) {
				if (occupied(piece, rotation, px, py) && pieceY + py >= 0)
					drawCell(g, pieceX + px, pieceY + py, COLORS[piece]);
			}
		}
		int panelX = COLS * CELL + 20;
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString("TETRIS", panelX, 42);
		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		g.drawString("Puntos: " + score, panelX, 90);
		g.drawString("Líneas: " + lines, panelX, 120);
		g.drawString("Nivel: " + level, panelX, 150);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g.drawString("← →  Mover", panelX, 220);
		g.drawString("↓     Bajar", panelX, 245);
		g.drawString("↑ / X  Girar", panelX, 270);
		g.drawString("Espacio  Caída", panelX, 295);
		g.drawString("P       Pausa", panelX, 320);
		g.drawString("R       Reiniciar", panelX, 345);
		if (paused || gameOver) {
			g.setColor(new Color(0, 0, 0, 175));
			g.fillRect(0, 0, COLS * CELL, ROWS * CELL);
			g.setColor(Color.WHITE);
			g.setFont(new Font("SansSerif", Font.BOLD, 28));
			String message = gameOver ? "FIN DEL JUEGO" : "PAUSA";
			int width = g.getFontMetrics().stringWidth(message);
			g.drawString(message, (COLS * CELL - width) / 2, ROWS * CELL / 2);
			g.setFont(new Font("SansSerif", Font.PLAIN, 16));
			String hint = gameOver ? "Pulsa R para reiniciar" : "Pulsa P para continuar";
			width = g.getFontMetrics().stringWidth(hint);
			g.drawString(hint, (COLS * CELL - width) / 2, ROWS * CELL / 2 + 32);
		}
		g.dispose();
	}

	private void drawCell(Graphics2D g, int x, int y, Color color) {
		g.setColor(color);
		g.fillRoundRect(x * CELL + 2, y * CELL + 2, CELL - 4, CELL - 4, 6, 6);
		g.setColor(color.brighter());
		g.drawLine(x * CELL + 5, y * CELL + 5, x * CELL + CELL - 6, y * CELL + 5);
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Tetris");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setResizable(false);
			frame.add(new tetris());
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
