package com.solwazi.slidemahjong;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/**
 * Hosts the board: title, tile counter, BoardView, and the two buttons.
 */
public class MainActivity extends Activity {

    private Board board;
    private BoardView boardView;
    private TextView tileCountView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        board = new Board();
        boardView = findViewById(R.id.board_view);
        tileCountView = findViewById(R.id.tile_count);

        boardView.setBoard(board);
        boardView.setBoardListener(new BoardView.BoardListener() {
            @Override
            public void onTileCountChanged(int remaining) {
                tileCountView.setText(getString(R.string.tiles_remaining, remaining));
            }

            @Override
            public void onBoardCleared() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.win_title)
                        .setMessage(R.string.win_message)
                        .setPositiveButton(R.string.win_ok, null)
                        .show();
            }
        });

        Button newBoardButton = findViewById(R.id.btn_new_board);
        newBoardButton.setOnClickListener(v -> boardView.newBoard());

        Button hintButton = findViewById(R.id.btn_hint);
        hintButton.setOnClickListener(v -> boardView.showHint());

        boardView.newBoard();
    }
}
