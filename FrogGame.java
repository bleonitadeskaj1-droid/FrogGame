import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FrogGame extends JPanel implements ActionListener, MouseListener {
    private static final int GAME_WIDTH = 700;
    private static final int GAME_HEIGHT = 500;
    private static final int TONGUE_REACH = 180;
    private static final int TONGUE_SPEED = 140;
    
    private final Timer gameTimer;
    private final Random random = new Random();
    
    // Game state
    private int score = 0;
    private int misses = 0;
    private int timeLeft = 90;
    private boolean gameActive = false;
    private long startTime = 0;
    
    // Frog
    private final int frogX = WIDTH / 2;
    private final int frogY = HEIGHT / 2;
    private int tongueLength = 0;
    private int tongueTargetX = 0;
    private int tongueTargetY = 0;
    private boolean tongueExtending = false;
    private boolean tongueRetracting = false;
    private Timer tongueTimer;
    
    // Lists
    private final ArrayList<Bug> bugs = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();
    
    public FrogGame() {
        setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        setBackground(new Color(42, 95, 127));
        
        this.gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
        
        startGame();
        
        SwingUtilities.invokeLater(() -> addMouseListener(this));
    }
    
    private void startGame() {
        score = 0;
        misses = 0;
        timeLeft = 90;
        gameActive = true;
        startTime = System.currentTimeMillis();
        
        bugs.clear();
        particles.clear();
        
        // Add initial flies
        for (int i = 0; i < 4; i++) {
            bugs.add(new Bug("fly"));
        }
        
        // Spawn more flies periodically
        Timer flySpawner = new Timer(5000, e -> {
            if (gameActive && countFlies() < 7) {
                bugs.add(new Bug("fly"));
            }
        });
        flySpawner.start();
        
        // Spawn bees periodically
        Timer beeSpawner = new Timer(10000, e -> {
            if (gameActive && random.nextDouble() < 0.3) {
                bugs.add(new Bug("bee"));
            }
        });
        beeSpawner.start();
    }
    
    private int countFlies() {
        int count = 0;
        for (Bug bug : bugs) {
            if (bug.type.equals("fly")) count++;
        }
        return count;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameActive) {
            // Update timer
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            timeLeft = Math.max(0, 90 - (int) elapsed);
            
            if (timeLeft == 0 && score < 3) {
                endGame(false);
            }
            
            // Update bugs
            for (int i = bugs.size() - 1; i >= 0; i--) {
                bugs.get(i).update();
            }
            
            // Update particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                particles.get(i).update();
                if (particles.get(i).life <= 0) {
                    particles.remove(i);
                }
            }
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw water background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(42, 95, 127), 
                                                    0, GAME_HEIGHT, new Color(26, 58, 95));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        
        // Draw lily pads
        g2d.setColor(new Color(34, 139, 34, 25));
        for (int i = 0; i < 5; i++) {
            int x = (int) ((i + 0.5) * (GAME_WIDTH / 5.0));
            int y = (int) (GAME_HEIGHT * 0.8);
            g2d.fillOval(x - 40, y - 30, 80, 60);
        }
        
        // Draw bugs
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        for (Bug bug : bugs) {
            bug.draw(g2d);
        }
        
        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }
        
        // Draw frog tongue
        if (tongueLength > 0) {
            double angle = Math.atan2(tongueTargetY - frogY, tongueTargetX - frogX);
            int endX = (int) (frogX + Math.cos(angle) * tongueLength);
            int endY = (int) (frogY + Math.sin(angle) * tongueLength);
            
            g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 20, 147));
            g2d.drawLine(frogX, frogY, endX, endY);
            
            g2d.setColor(new Color(255, 105, 180));
            g2d.fillOval(endX - 6, endY - 6, 12, 12);
        }
        
        // Draw frog
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        g2d.drawString("🐸", frogX - 30, frogY + 20);
        
        // Draw catch radius
        if (gameActive && !tongueExtending && !tongueRetracting) {
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                                         0, new float[]{5}, 0));
            g2d.drawOval(frogX - TONGUE_REACH, frogY - TONGUE_REACH, 
                        TONGUE_REACH * 2, TONGUE_REACH * 2);
        }
        
        // Draw UI
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        g2d.drawString("🎯 Score: " + score + "/3", 20, 30);
        g2d.drawString("❌ Misses: " + misses + "/5", 250, 30);
        g2d.drawString("⏱️ Time: " + timeLeft + "s", 500, 30);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameActive || tongueExtending || tongueRetracting) return;
        
        int clickX = e.getX();
        int clickY = e.getY();
        
        double dx = clickX - frogX;
        double dy = clickY - frogY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        if (dist <= TONGUE_REACH) {
            tongueTargetX = clickX;
            tongueTargetY = clickY;
            tongueExtending = true;
            shootTongue();
        }
    }
    
    private void shootTongue() {
        double dx = tongueTargetX - frogX;
        double dy = tongueTargetY - frogY;
        final double targetDist = Math.sqrt(dx * dx + dy * dy);
        
        tongueTimer = new Timer(16, e -> {
            if (tongueExtending && tongueLength < targetDist) {
                tongueLength = (int) Math.min(targetDist, tongueLength + TONGUE_SPEED);
            } else if (tongueExtending) {
                tongueTimer.stop();
                checkHit();
                retractTongue();
            } else if (tongueRetracting && tongueLength > 0) {
                tongueLength = Math.max(0, tongueLength - TONGUE_SPEED);
            } else if (tongueRetracting) {
                tongueLength = 0;
                tongueRetracting = false;
                tongueTimer.stop();
            }
        });
        tongueTimer.start();
    }
    
    private void checkHit() {
        double angle = Math.atan2(tongueTargetY - frogY, tongueTargetX - frogX);
        int tongueEndX = (int) (frogX + Math.cos(angle) * tongueLength);
        int tongueEndY = (int) (frogY + Math.sin(angle) * tongueLength);
        
        Bug hitBug = null;
        for (Bug bug : bugs) {
            if (bug.isHit(tongueEndX, tongueEndY)) {
                hitBug = bug;
                break;
            }
        }
        
        if (hitBug != null) {
            if (hitBug.type.equals("fly")) {
                score++;
                // Create particles
                for (int i = 0; i < 12; i++) {
                    double pAngle = (i / 12.0) * Math.PI * 2;
                    particles.add(new Particle(hitBug.x, hitBug.y, 
                        Math.cos(pAngle) * 3, Math.sin(pAngle) * 3));
                }
                if (score >= 3) {
                    endGame(true);
                }
            } else {
                endGame(false);
            }
            bugs.remove(hitBug);
        } else {
            misses++;
            if (misses >= 5) {
                endGame(false);
            }
        }
    }
    
    private void retractTongue() {
        tongueExtending = false;
        tongueRetracting = true;
    }
    
    private void endGame(boolean won) {
        gameActive = false;
        String message = won ? "YOU WIN! 🏆\nScore: " + score : 
                              "GAME OVER 💀\nScore: " + score + "/3";
        JOptionPane.showMessageDialog(this, message);
        startGame();
    }
    
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    
    // Bug class
    class Bug {
        String type;
        double x, y, vx, vy;
        
        Bug(String type) {
            this.type = type;
            reset();
        }
        
        final void reset() {
            int side = random.nextInt(4);
            switch (side) {
                case 0 -> {
                    x = random.nextDouble() * GAME_WIDTH;
                    y = -20;
                }
                case 1 -> {
                    x = GAME_WIDTH + 20;
                    y = random.nextDouble() * GAME_HEIGHT;
                }
                case 2 -> {
                    x = random.nextDouble() * GAME_WIDTH;
                    y = GAME_HEIGHT + 20;
                }
                default -> {
                    x = -20;
                    y = random.nextDouble() * GAME_HEIGHT;
                }
            }
            
            double angle = Math.atan2(frogY - y, frogX - x);
            double speed = type.equals("fly") ? 2.6 : 2.2;
            vx = Math.cos(angle) * speed + (random.nextDouble() - 0.5) * 1.2;
            vy = Math.sin(angle) * speed + (random.nextDouble() - 0.5) * 1.2;
        }
        
        void update() {
            x += vx;
            y += vy;
            
            if (x < -50) x = GAME_WIDTH + 50;
            if (x > GAME_WIDTH + 50) x = -50;
            if (y < -50) y = GAME_HEIGHT + 50;
            if (y > GAME_HEIGHT + 50) y = -50;
        }
        
        void draw(Graphics2D g) {
            g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, type.equals("fly") ? 24 : 28));
            String emoji = type.equals("fly") ? "🪰" : "🐝";
            g.drawString(emoji, (int) x - 12, (int) y + 8);
        }
        
        boolean isHit(int tx, int ty) {
            double dx = x - tx;
            double dy = y - ty;
            return Math.sqrt(dx * dx + dy * dy) < 35;
        }
    }
    
    // Particle class
    class Particle {
        double x, y, vx, vy;
        int life = 30;
        
        Particle(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.3;
            life--;
        }
        
        void draw(Graphics2D g) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, life / 30f));
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(random.nextBoolean() ? "✨" : "💫", (int) x, (int) y);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Frog Feast 🐸");
        FrogGame game = new FrogGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}