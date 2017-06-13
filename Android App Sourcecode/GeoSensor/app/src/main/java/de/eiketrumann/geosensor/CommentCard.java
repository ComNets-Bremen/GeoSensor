package de.eiketrumann.geosensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * A card for a card view that shows a comment string and a button to edit it
 * Created by Eike on 18.02.2017.
 */

@SuppressLint("ViewConstructor")
public class CommentCard extends CardView {
    /** The DataRecord this card refers to, is used to store changed comments*/
    private final DataRecord dataRecord;
    /** The activity context containing this card. Used to show the edit alert dialog*/
    private final Context context;

    /**
     * Instantiates a card that shows the comment for the DataRecord given.
     * @param context The activity context showing this card
     * @param dataRecord The DataRecord containing the comment to be shown
     */
    CommentCard(Context context, DataRecord dataRecord){
        super(context);
        this.dataRecord = dataRecord;
        this.context = context;
        init();
    }

    /**
     * This method initializes the card. It inflates the xml and contains the code to show an alert
     * dialog allowing the user to edit the comment.
     */
    private void init(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View card = inflater.inflate(R.layout.comment_card, this);
        final TextView comment = (TextView) card.findViewById(R.id.card_comment_content);

        comment.setText(dataRecord.getComment());

        // When the button is touched, an AlertDialog containing an EditText is shown
        ImageButton imageButton = (ImageButton) card.findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setTitle(context.getString(R.string.edit_comment_title));
                final EditText editText = new EditText(context);
                editText.setText(dataRecord.getComment());
                alertDialog.setView(editText);

                // When OK is pressed, the comment is saved and the dialog closed
                alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dataRecord.setComment(context, editText.getText().toString());
                        comment.setText(editText.getText().toString());
                    }
                });

                // If the cancel button is pressed, nothing is changed and the dialog is dismissed
                alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                alertDialog.show();
            }
        });
    }
}

