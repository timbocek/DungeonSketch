package com.tbocek.android.combatmap.about;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.CombatMap;
import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.view.TokenButton;

/**
 * Activity that loads and displays art credits for each built-in token.
 * 
 * @author Tim
 * 
 */
public class ArtCredits extends Activity {

    /**
     * View to display art credit info; credit data will be dynamically added
     * here.
     */
    private ArtCreditsView mCreditsView;
    private TokenImageManager.Loader mLoader;
    List<TokenButton> mTokenButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.art_credits);
        FrameLayout frame =
                (FrameLayout) this.findViewById(R.id.art_credits_frame);
        this.mCreditsView = new ArtCreditsView(this);
        this.mCreditsView
                .setTokenButtonClickListener(new ArtCreditsView.TokenButtonClickListener() {
                    @Override
                    public void onTokenButtonClick(String url) {
                        if (url != null) {
                            Intent browserIntent =
                                    new Intent(Intent.ACTION_VIEW, Uri
                                            .parse(url));
                            ArtCredits.this.startActivity(browserIntent);
                        }
                    }
                });
        ArtCreditHandler handler = new ArtCreditHandler();

        mLoader = new TokenImageManager.Loader(this, new Handler());
        mLoader.start();
        mLoader.getLooper(); // Make sure loader thread is ready to go.

        try {
            InputStream is =
                    this.getResources().openRawResource(R.raw.art_credits);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));
            is.close();


        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTokenButtons = handler.getCreatedTokenButtons();

        frame.addView(this.mCreditsView);
    }

    @Override
    public void onResume() {
        super.onResume();
        TokenImageManager mgr = TokenImageManager.getInstance(this);
        for (final TokenButton b: mTokenButtons) {
            mgr.requireTokenImage(b.getTokenId(), mLoader, new TokenImageManager.Callback() {
                @Override
                public void imageLoaded(String tokenId) {
                    b.invalidate();;
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLoader.clearQueue();
        mLoader.quit();
        // TODO: Clean up any loaded tokens.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, CombatMap.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * SAX handler that parses the art credit file and sets up the credits view.
     * 
     * @author Tim
     * 
     */
    private class ArtCreditHandler extends DefaultHandler {

        /**
         * List that accumulates all token buttons created to display art
         * credits; can be used to load their images as a batch.
         */
        private List<TokenButton> mCreatedTokenButtons = Lists.newArrayList();

        /**
         * Name of the artist currently being parsed.
         */
        private String mCurrentArtist;

        /**
         * @return List of all token buttons created as a result of the walk.
         */
        public List<TokenButton> getCreatedTokenButtons() {
            return this.mCreatedTokenButtons;
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                String qName, org.xml.sax.Attributes atts) throws SAXException {
            if (localName.equalsIgnoreCase("artist")) {
                this.mCurrentArtist = atts.getValue("name");
                ArtCredits.this.mCreditsView.addArtist(this.mCurrentArtist,
                        atts.getValue("copyright"), atts.getValue("url"));
            } else if (localName.equalsIgnoreCase("token")) {
                this.mCreatedTokenButtons.add(ArtCredits.this.mCreditsView
                        .addArtCredit(this.mCurrentArtist,
                                atts.getValue("res"), atts.getValue("url")));
            }
        }
    }
}
