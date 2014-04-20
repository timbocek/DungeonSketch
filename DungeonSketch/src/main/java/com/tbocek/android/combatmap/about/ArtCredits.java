package com.tbocek.android.combatmap.about;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

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
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.CombatMap;
import com.tbocek.android.combatmap.TokenImageManager;
import com.tbocek.android.combatmap.view.ListeningScrollView;
import com.tbocek.dungeonsketch.R;
import com.tbocek.android.combatmap.view.TokenButton;

/**
 * Activity that loads and displays art credits for each built-in token.
 * 
 * @author Tim
 * 
 */
public class ArtCredits extends Activity {
    private static final String TAG="ArtCredits";

    /**
     * View to display art credit info; credit data will be dynamically added
     * here.
     */
    private ArtCreditsView mCreditsView;
    private TokenImageManager.Loader mLoader;
    List<TokenButton> mTokenButtons;
    ListeningScrollView mScrollView;

    Set<String> mVisibleTokens = Sets.newHashSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.art_credits);
        final FrameLayout frame =
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

        this.mScrollView = (ListeningScrollView)findViewById(R.id.art_credits_scroll_view);
        mScrollView.setOnScrollChangedListener(new ListeningScrollView.OnScrollChangedListener() {
            @Override
            public void OnScrollChanged(ScrollView view, int x, int y, int oldx, int oldy) {
                changeTokenImages();
            }
        });

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

        final ViewTreeObserver vto = frame.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                changeTokenImages();
                frame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
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

    private void changeTokenImages() {
        Set<String> newVisibleIds = Sets.newHashSet();
        int height = this.getWindowManager().getDefaultDisplay().getHeight();
        int location[] = new int[2];
        TokenImageManager mgr = TokenImageManager.getInstance(this);
        // Determine if each token is on screen.  Load images for only the ones that are.
        for (final TokenButton b: mTokenButtons) {
            b.getLocationOnScreen(location);
            if (location[1] < height && location[1] + b.getHeight() > 0 ) {  // Control is visible
                newVisibleIds.add(b.getTokenId());
                if (!mVisibleTokens.contains(b.getTokenId())) {
                    mgr.requireTokenImage(b.getTokenId(), mLoader, new TokenImageManager.Callback() {
                        @Override
                        public void imageLoaded(String tokenId) {
                            b.invalidate();
                        }
                    });
                }
            }
        }


        for (String unusedId: Sets.difference(mVisibleTokens, newVisibleIds)) {
            Log.i("ArtCredits", "Token just went off screen: " + unusedId);
            mLoader.discardOrCancelTokenLoad(unusedId);
        }
        mVisibleTokens = newVisibleIds;
    }
}
