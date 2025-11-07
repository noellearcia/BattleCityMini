import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class BattleCityMini extends JPanel implements ActionListener, KeyListener {
    static final int WIDTH = 800, HEIGHT = 600;
    static final int TILE_SIZE = 40;
    static final int ROWS = HEIGHT / TILE_SIZE;
    static final int COLS = WIDTH / TILE_SIZE;

    static final int PLAYER_SIZE = 30;
    static final int ENEMY_SIZE = 30;
    static final int BULLET_SIZE = 6;

    javax.swing.Timer timer;
    Tank player;
    ArrayList<Tank> enemies;
    ArrayList<Bullet> bullets;
    Random rand = new Random();

    int[][] maze; // 0 = empty, 1 = brick, 2 = steel

    public BattleCityMini() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        generateMaze();

        player = new Tank(WIDTH / 2, HEIGHT - 60, Color.GREEN);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();

        // Spawn enemies
        for (int i = 0; i < 5; i++) {
            int ex, ey;
            do {
                ex = rand.nextInt(COLS) * TILE_SIZE;
                ey = rand.nextInt(ROWS / 2) * TILE_SIZE;
            } while (maze[ey / TILE_SIZE][ex / TILE_SIZE] != 0);
            enemies.add(new Tank(ex, ey, Color.RED));
        }

        timer = new javax.swing.Timer(30, this);
        timer.start();
    }

    private void generateMaze() {
        maze = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (r == 0 || c == 0 || r == ROWS - 1 || c == COLS - 1)
                    maze[r][c] = 2;
                else {
                    double rnd = rand.nextDouble();
                    if (rnd < 0.10) maze[r][c] = 1;
                    else if (rnd < 0.13) maze[r][c] = 2;
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw maze
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (maze[r][c] == 1) {
                    g.setColor(new Color(178, 34, 34)); // brick
                    g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (maze[r][c] == 2) {
                    g.setColor(Color.GRAY); // steel
                    g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        player.draw(g);
        for (Tank e : enemies) e.draw(g);
        for (Bullet b : bullets) b.draw(g);

        g.setColor(Color.WHITE);
        g.drawString("Enemies left: " + enemies.size(), 10, 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        player.moveDir(player.dir, maze); // Move player automatically
        moveEnemies();
        moveBullets();
        checkCollisions();
        repaint();
    }

    private void moveEnemies() {
        for (Tank enemy : enemies) {
            if (rand.nextInt(50) == 0) enemy.dir = rand.nextInt(4);
            enemy.moveDir(enemy.dir, maze);
        }
    }

    private void moveBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.move();

            int r = b.y / TILE_SIZE;
            int c = b.x / TILE_SIZE;

            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                if (maze[r][c] == 1) {
                    maze[r][c] = 0; // destroy brick
                    it.remove();
                    continue;
                } else if (maze[r][c] == 2) {
                    it.remove(); // steel stops bullet
                    continue;
                }
            }

            if (b.x < 0 || b.x > WIDTH || b.y < 0 || b.y > HEIGHT)
                it.remove();
        }
    }

    private void checkCollisions() {
        Iterator<Tank> et = enemies.iterator();
        while (et.hasNext()) {
            Tank enemy = et.next();
            Rectangle er = new Rectangle(enemy.x, enemy.y, ENEMY_SIZE, ENEMY_SIZE);
            for (Bullet b : new ArrayList<>(bullets)) {
                Rectangle br = new Rectangle(b.x, b.y, BULLET_SIZE, BULLET_SIZE);
                if (er.intersects(br)) {
                    et.remove();
                    bullets.remove(b);
                    break;
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT) player.dir = 0;
        if (k == KeyEvent.VK_RIGHT) player.dir = 1;
        if (k == KeyEvent.VK_UP) player.dir = 2;
        if (k == KeyEvent.VK_DOWN) player.dir = 3;
        if (k == KeyEvent.VK_SPACE) bullets.add(player.shoot());
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    static class Tank {
        int x, y, dir;
        Color color;
        static final int SPEED = 4;

        Tank(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void moveDir(int dir, int[][] maze) {
            int nx = x, ny = y;
            switch (dir) {
                case 0: nx -= SPEED; break;
                case 1: nx += SPEED; break;
                case 2: ny -= SPEED; break;
                case 3: ny += SPEED; break;
            }
            if (!collidesWithWall(nx, ny, maze)) {
                x = nx;
                y = ny;
            }
        }

        private boolean collidesWithWall(int nx, int ny, int[][] maze) {
            Rectangle tankRect = new Rectangle(nx, ny, PLAYER_SIZE, PLAYER_SIZE);
            int startRow = Math.max(0, ny / TILE_SIZE);
            int endRow = Math.min(ROWS - 1, (ny + PLAYER_SIZE) / TILE_SIZE);
            int startCol = Math.max(0, nx / TILE_SIZE);
            int endCol = Math.min(COLS - 1, (nx + PLAYER_SIZE) / TILE_SIZE);
            for (int r = startRow; r <= endRow; r++) {
                for (int c = startCol; c <= endCol; c++) {
                    if (maze[r][c] != 0) {
                        Rectangle wall = new Rectangle(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        if (tankRect.intersects(wall))
                            return true;
                    }
                }
            }
            return false;
        }

        Bullet shoot() {
            int bx = x + PLAYER_SIZE / 2;
            int by = y + PLAYER_SIZE / 2;
            return new Bullet(bx, by, dir);
        }

        void draw(Graphics g) {
            g.setColor(color);
            g.fillRect(x, y, PLAYER_SIZE, PLAYER_SIZE);
            g.setColor(Color.WHITE);
            int cx = x + PLAYER_SIZE / 2;
            int cy = y + PLAYER_SIZE / 2;
            int len = 20;
            switch (dir) {
                case 0: g.drawLine(cx, cy, cx - len, cy); break;
                case 1: g.drawLine(cx, cy, cx + len, cy); break;
                case 2: g.drawLine(cx, cy, cx, cy - len); break;
                case 3: g.drawLine(cx, cy, cx, cy + len); break;
            }
        }
    }

    static class Bullet {
        int x, y, dir;
        static final int SPEED = 8;

        Bullet(int x, int y, int dir) {
            this.x = x;
            this.y = y;
            this.dir = dir;
        }

        void move() {
            switch (dir) {
                case 0: x -= SPEED; break;
                case 1: x += SPEED; break;
                case 2: y -= SPEED; break;
                case 3: y += SPEED; break;
            }
        }

        void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, BULLET_SIZE, BULLET_SIZE);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Battle City Maze");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
        f.add(new BattleCityMini());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
