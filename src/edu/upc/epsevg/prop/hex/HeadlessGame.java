package edu.upc.epsevg.prop.hex;



import edu.upc.epsevg.prop.hex.players.H_E_X_Player;
import edu.upc.epsevg.prop.hex.players.HumanPlayer;
import edu.upc.epsevg.prop.hex.players.RandomPlayer;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bernat
 */
public class HeadlessGame {

    private IPlayer players[];
    private HexGameStatus status;
    private int gameCount;
    private int timeout;
    private int size;
    
    public static void main(String[] args) {


        IPlayer player1 = new RandomPlayer("Paco");
        IPlayer player2 = new H_E_X_Player(2/*GB*/);                    
        
        HeadlessGame game = new HeadlessGame(player1, player2, 9, 5/*s timeout*/, 10/*games*/);
        GameResult gr = game.start();
        System.out.println(gr);

    }

    //=====================================================================================0
    public HeadlessGame(IPlayer p1, IPlayer p2, int size, int timeout, int gameCount) {
        this.size = size;
        this.players = new IPlayer[2];
        players[0] = p1;
        players[1] = p2;
        this.gameCount = gameCount;
        this.timeout = timeout;
    }

    public GameResult start() {
        GameResult gr = new GameResult();
        for (int i = 0; i < gameCount; i++) {
            //System.out.println(">" + i);
            gr.update(play(players[0], players[1]));
        }
        return gr;
    }

    private class Result {
        public boolean ok;
    }

    private PlayerType play(IPlayer player, IPlayer player0) {
        this.status = new HexGameStatus(size);

        while (!this.status.isGameOver()) {

            final Semaphore semaphore = new Semaphore(1);
            semaphore.tryAcquire();
            //System.out.println("." + new Date());
            final Result r = new Result();
            PlayerType cp = status.getCurrentPlayer();
            Thread t1 = new Thread(() -> {
                PlayerMove m = null;
                try {
                    m = players[cp == PlayerType.PLAYER1 ? 0 : 1].move(new HexGameStatus(status));
                } catch(Exception ex) {
                    System.out.println("Excepció descontrolada al player:"+cp.name());
                    ex.printStackTrace();
                }
                if (m != null) {
                    status.placeStone(m.getPoint());
                } else {
                    status.forceLoser();
                }
                System.out.print(cp==PlayerType.PLAYER1?"1":"2");
                r.ok = true;
                semaphore.release();
            });

            Thread t2 = new Thread(() -> {
                try {
                    Thread.sleep(HeadlessGame.this.timeout * 1000);
                } catch (InterruptedException ex) {
                }
                if (!r.ok) {
                    players[cp == PlayerType.PLAYER1 ? 0 : 1].timeout();
                }
            });

            t1.start();
            t2.start();
            long WAIT_EXTRA_TIME = 2000;
            try {
                if (!semaphore.tryAcquire(1, timeout * 1000 + WAIT_EXTRA_TIME, TimeUnit.MILLISECONDS)) {

                    System.out.println("Espera il·legal ! Player trampós:"+cp.name());
                    //throw new RuntimeException("Jugador trampós ! Espera il·legal !");
                    // Som millors persones deixant que el jugador il·legal continui jugant...
                    semaphore.acquire();
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(HeadlessGame.class.getName()).log(Level.SEVERE, null, ex);
            }
            // Netegem la memòria (for free!)
            gc();
            
        }
        return status.winnerPlayer;
    }

    private class GameResult {

        java.util.List<PlayerType> results;

        public GameResult() {
            results = new ArrayList<PlayerType>();

        }

        public void update(PlayerType res) {
            results.add(res);
        }

        @Override
        public String toString() {
            String res = "\n ================================================================="+
                         "\n ================       RESULTS       ============================"+
                         "\n =================================================================\n";
            int wins1 = 0, ties1 = 0, loose1 = 0;
            for (PlayerType c : results) {
                if (null == c) {
                    ties1++;
                } else {
                    switch (c) {
                        case PLAYER1:
                            wins1++;
                            break;
                        default:
                            loose1++;
                            break;
                    }
                }
            }

            res += "PLAYER 1 (" + pad(players[0].getName(), 40) + "):\t wins " + wins1 + "\t ties:" + ties1 + "\t looses:" + loose1 + "\n";
            res += "PLAYER 2 (" + pad(players[1].getName(), 40) + "):\t wins " + loose1 + "\t ties:" + ties1 + "\t looses:" + wins1 + "\n";
            return res;
        }

        public String pad(String inputString, int length) {
            if (inputString.length() >= length) {
                return inputString;
            }
            StringBuilder sb = new StringBuilder();
            while (sb.length() < length - inputString.length()) {
                sb.append(' ');
            }
            sb.append(inputString);

            return sb.toString();
        }
    }

    
    /**
     * This method guarantees that garbage collection is done unlike
     * <code>{@link System#gc()}</code>
     */
    public static void gc() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<Object>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }
}
    