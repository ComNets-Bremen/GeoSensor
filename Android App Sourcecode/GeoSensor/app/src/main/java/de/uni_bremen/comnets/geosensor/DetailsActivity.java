package de.uni_bremen.comnets.geosensor;

import android.content.DialogInterface;
import android.location.Location;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * The details activity is a card view containing cards for all information available reagading a
 * single Data Record.
 *
 * @author Eike Trumann
 */
public class DetailsActivity extends GeoSensorActivity {
    private DataRecord dataRecord;

    /**
     * Set the layout and toolbar, read the record from the database and add the appropriate cards.
     * @param savedInstanceState Android lifecycle Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        DataLab dl = new DataLab(this);
        long elementID = getIntent().getLongExtra("id",0);
        dataRecord = dl.readDataRecordFromDatabase(elementID);

        // The ID given does not exist in the database
        if(dataRecord == null){
            Toast.makeText(this, getString(R.string.entry_does_not_exist), Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }


        LinearLayout layout = (LinearLayout) findViewById(R.id.details_card_view);

        layout.addView(new GeneralInfoCard(this,dataRecord));

        layout.addView(new CommentCard(this,dataRecord));

        for(Location location : dataRecord.getLocations()){
            layout.addView(new PositionCard(this,location));
        }

        if(Utils.hasInternet(this) && !dataRecord.getLocations().isEmpty()){
            layout.addView(new MapCard(this,dataRecord));
        }

        for(MeasureData measureData : dataRecord.getMeasureData()){
            layout.addView(new MeasureDataCard(this,measureData));
        }
    }

    /**
     * Add a back arrow to the toolbar, show the delete button and hide the sort button.
     * @param menu The toolbar
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_sort).setVisible(false);
        menu.findItem(R.id.action_delete).setVisible(true);

        // this shows the back arrow in the top left corner
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } catch (NullPointerException e) {
            Log.w(this.getClass().getSimpleName(),e);
        }
        return true;
    }

    /**
     * Handle the action items in the toolbar that are not handled by super.
     * In this case, handle the delete button.
     * @param item The item selected
     * @return true if the action has been handled
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Show a conformation dialog regarding the deletion of the data record.
     * If the user accepts, the data record represented by this Activity is deleted and the
     * Activity is closed
     */
    private void showDeleteDialog(){
        AlertDialog.Builder deleteMessageBuilder = new AlertDialog.Builder(DetailsActivity.this,R.style.DialogTheme);
        deleteMessageBuilder.setTitle(getString(R.string.deleteEntry));
        deleteMessageBuilder.setMessage(getString(R.string.deleteMessage));

        deleteMessageBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DataLab dl = new DataLab(DetailsActivity.this);
                dl.deleteDataRecord(dataRecord.getDatabaseID());
                finish();
            }
        });
        deleteMessageBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which){}
        });

        AlertDialog deleteMessage = deleteMessageBuilder.create();
        deleteMessage.setCanceledOnTouchOutside(true);
        deleteMessage.show();
    }
}
