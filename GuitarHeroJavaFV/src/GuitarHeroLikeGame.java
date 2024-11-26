import javax.swing.*;
import javax.sound.sampled.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class GuitarHeroLikeGame extends JPanel implements ActionListener {
    private Timer timer;

    // Lista delle note presenti a schermo
    private List<Note> notes;

    // Punteggio dell'utente
    private int score;

    // Velocità delle note
    private int noteSpeed;

    // Vite attuali dell'utente
    private int lives;

    // Font del punteggio
    private Font scoreFont;

    // Velocità massima che le note possono raggiungere (Difficoltà massima)
    private final int MAX_NOTE_SPEED = 10;
    private Random random;

    // Numero di vite totali dell'utente
    private final int MAX_LIVES = 3;

    // Boolean per mostrare a schermo l'animazione di una vita persa
    private boolean showLifeLoss;

    // Timer utilizzato per gestire l'animazione della vita persa
    private int lifeLossTimer;

    // Array di boolean utilizzato per tenere traccia dei tasti premuti
    private boolean[] keyPressed;

    // Utilizzato per la mutua esclusione dell'array di note
    private final Object lock = new Object();

    // Gestione della velocità dello spawn delle note
    private int spawnDelay;
    private Timer spawnTimer;


    public GuitarHeroLikeGame() {

        setFocusable(true);

        // Dimensione della finestra e set dello sfondo di gioco
        setPreferredSize(new Dimension(400, 600));
        setBackground(Color.BLACK);

        // Inizializzo le variabili di gioco
        notes = new ArrayList<>();
        score = 0;
        noteSpeed = 2;
        lives = MAX_LIVES;
        random = new Random();
        showLifeLoss = false;
        lifeLossTimer = 0;

        // Controllo per quali tasti vengono premuti inizializzato a 3 (A, S, D)
        keyPressed = new boolean[3];

        // Inizializzazione del timer
        timer = new Timer(10, this);
        timer.start();

        // Set del font per il punteggio
        scoreFont = new Font("Arial", Font.BOLD, 18);

        // Creo un listener associandoci gli eventi del tasto premuto e rilasciato per tenere traccia dei tasti premuti
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                if (keyChar == 'a' || keyChar == 's' || keyChar == 'd') {
                    int index = "asd".indexOf(keyChar);
                    keyPressed[index] = true; // Aggiorna lo stato del tasto
                    checkHit(keyChar); // Chiama sempre checkHit quando il tasto è premuto
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                if (keyChar == 'a' || keyChar == 's' || keyChar == 'd') {
                    int index = "asd".indexOf(keyChar);
                    keyPressed[index] = false;
                }
            }
        });

        // Spawn delle note
        spawnDelay = 2000;
        initSpawnTimer();
    }

    // Gestione del timer associato allo spawn delle note dinamico in base alla difficoltà
    private void initSpawnTimer(){
        if(spawnTimer != null){
            spawnTimer.stop();
        }

        spawnTimer = new Timer(spawnDelay, e -> spawnNotes());
        spawnTimer.start();
    }

    // Spawn delle note
    private void spawnNotes() {

        // Aumenta il numero massimo di note con la difficoltà
        int maxNotes = 1 + noteSpeed / 2;

        // Controllo che almeno una nota venga generata
        int numberOfNotes = random.nextInt(maxNotes) + 1;
        for (int i = 0; i < numberOfNotes; i++) {
            notes.add(new Note());
        }
    }

    // Metodo che controlla - ogni volta che un tasto viene premuto - se una nota viene colpita
    private void checkHit(char keyChar) {

        // utilizzo di un object come lock per garantire la mutua esclusione sulla lista di note
        synchronized (lock) {
            boolean noteFound = false;
            Iterator<Note> iterator = notes.iterator();
            while (iterator.hasNext()) {
                Note note = iterator.next();

                // se la nota è colpibile ed è nella stessa 'corsia' del tasto premuto allora quest'ultima viene rimossa
                // il punteggio aumentato e viene fatto il controllo sull'incremento di difficoltà
                if (note.isHittable() && note.getKeyChar() == keyChar) {
                    iterator.remove();
                    score += 10;
                    increaseDifficulty();
                    System.out.println("Nota colpita e rimossa: " + keyChar);
                    new Thread(() -> playSound(keyChar + ".wav")).start();
                    noteFound = true;
                    // Non usiamo 'return' qui, così continuiamo a verificare altre note
                }
            }
            // Se non viene trovata alcuna nota, viene printata una semplice istruzione di debug
            if (!noteFound) {
                System.out.println("Nessuna nota colpibile trovata per: " + keyChar);
            }
        }
        // Aggiorna la grafica dopo aver rimosso le note, onde evitare errori di sincronizzazione
        repaint();
    }

    // Metodo che si occupa della rimozione di una vita e del controllo sul game over
    private void loseLife() {
        lives--;
        showLifeLoss = true;

        // Mostra l'animazione della perdita di vita per il periodo specificato
        lifeLossTimer = 50;

        // Controllo sul numero di vite per il game over
        if (lives <= 0) {
            gameOver();
        }
    }

    // Metodo che gestisce la riproduzione del suono qualora una nota venga colpita
    private void playSound(String soundFile) {
        try {
            System.out.println("Riproduzione del file: " + soundFile);
            // Ottengo l'indirizzo al file da riprodurre attraverso una stringa
            URL soundUrl = getClass().getClassLoader().getResource(soundFile);

            // Controllo e riproduzione effettiva dell'audio attraverso l'AudioInputStream
            if (soundUrl != null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundUrl);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                System.err.println("File audio non trovato: " + soundFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Metodo che gestisce l'incremento di difficoltà
    // Quindi l'aumento della velocità delle note, l'aumento della velocità dello spawn delle note e l'aumento delle
    // note spawnate in contemporanea
    private void increaseDifficulty() {
        if (score % 100 == 0) {
            if (noteSpeed < MAX_NOTE_SPEED) {
                noteSpeed++;
            }
            if (spawnDelay > 500) {
                spawnDelay -= 200;
                initSpawnTimer();
                System.out.println("Nuovo spawnDelay: " + spawnDelay);
            }
        }
    }

    // Metodo per gestire il gameover, mostrando a schermo un alert panel, che finisce per chiudere il gioco
    // non permettendo più al giocatore di poter giocare
    private void gameOver() {
        timer.stop();
        JOptionPane.showMessageDialog(this, "Game Over! Your score: " + score);
        System.exit(0);
    }

    // Collegato al timer associato, gestisce il movimento delle note sul piano di gioco
    @Override
    public void actionPerformed(ActionEvent e) {
        synchronized (lock) {
            Iterator<Note> iterator = notes.iterator();
            while (iterator.hasNext()) {
                Note note = iterator.next();

                // Movimento effettivo
                note.move(noteSpeed);

                if (note.isMissed()) {
                    iterator.remove();
                    loseLife();
                }
            }
        }

        if (showLifeLoss) {
            lifeLossTimer--;

            if (lifeLossTimer <= 0) {
                showLifeLoss = false;
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);


        // Disegna le note
        synchronized (lock) {
            for (Note note : notes) {
                note.draw(g);
            }
        }

        // Disegna le vite e il punteggio
        g.setColor(Color.WHITE);
        g.setFont(scoreFont);
        g.drawString("Score: " + score, 10, 30);
        g.drawString("Lives: " + lives, 10, 50);

        // Disegna l'animazione della perdita della vita
        if (showLifeLoss) {
            g.setColor(new Color(255, 0, 0, 128));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // Disegna la barra per le note per ogni corsia
        String keys = "ASD";
        int laneWidth = getWidth() / 3;
        for (int i = 0; i < 3; i++) {
            int x = i * laneWidth;

            if (keyPressed[i]) {
                g.setColor(getColorForKey(keys.charAt(i)));
                g.fillRoundRect(x + 10, 500, laneWidth - 20, 70, 20, 20);
            } else {
                g.setColor(Color.WHITE);
                g.drawRoundRect(x + 10, 500, laneWidth - 20, 70, 20, 20);
            }

            g.drawString("" + keys.charAt(i), x + laneWidth / 2 - 5, 590);
        }
    }

    // Metodo per la gestione del colore delle note in base alla loro corsia
    private Color getColorForKey(char keyChar) {
        switch (keyChar) {
            case 'a':
                return Color.RED;
            case 's':
                return Color.BLUE;
            case 'd':
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Guitar Hero Like Game");
        GuitarHeroLikeGame game = new GuitarHeroLikeGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    // Classe per la gestione delle note
    class Note {

        // Coordinate per la posizione
        private int y;
        private final int x;

        // Key associata al tasto da premere
        private final char keyChar;

        // Colore
        private final Color noteColor;

        public Note() {
            this.y = 0;
            int laneWidth = 400 / 3;
            int lane = random.nextInt(3); // Random lane between 0 and 2
            this.x = lane * laneWidth;
            this.keyChar = "asd".charAt(lane);
            this.noteColor = getColorForKey(keyChar);
        }

        // Gestione del movimento
        public void move(int speed) {
            y += speed;
        }

        // Metodo per il disegno di una nota
        public void draw(Graphics g) {
            int laneWidth = getWidth() / 3;
            g.setColor(noteColor);
            g.fillRoundRect(x + 10, y, laneWidth - 20, 30, 15, 15);
        }

        // Rende le noti hittable in un determinato intervallo
        public boolean isHittable() {
            return y > 470 && y < 600;
        }

        // Definisce quando una nota diventa persa
        public boolean isMissed() {
            return y > 600;
        }

        public char getKeyChar() {
            return keyChar;
        }
    }
}
