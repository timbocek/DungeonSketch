package com.tbocek.android.combatmap;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.BuiltInImageToken;
import com.tbocek.android.combatmap.model.primitives.CustomBitmapToken;
import com.tbocek.android.combatmap.model.primitives.LetterToken;
import com.tbocek.android.combatmap.model.primitives.PlaceholderToken;
import com.tbocek.android.combatmap.model.primitives.SolidColorToken;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.dungeonsketch.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Provides a lightweight database storing a list of tokens and allowing them to
 * be associated with tags.
 * 
 * @author Tim
 * 
 */
public final class TokenDatabase implements Cloneable {
	
	public static boolean isSystemTag(String tag) {
		int colonIndex = tag.indexOf(':');
		// All children of system tags are also system tags.
		if (colonIndex >= 0) {
			tag = tag.substring(0, colonIndex);
		}
		return SYSTEM_TAG_NAMES.contains(tag);
	}

    public static TokenDatabase getCopy() throws CloneNotSupportedException {
        return (TokenDatabase) instance.clone();
    }

    public class TagTreeNode implements Cloneable {
		/**
		 * Map of token names to the amount to deploy.  This is also just used
		 * as the set of tokens that are directly underneath this tag.
		 */
		private final Map<String, Integer> tokenCounts = Maps.newHashMap();
		
		/**
		 * Counts for tokens that should be provided a count in this tag,
		 * but are not necessarily direct children of this tag.  They are
		 * probably present in child tags; if they are not this is a
		 * problem and they should be cleaned up.
		 */
		private final Map<String, Integer> guestTokenCounts = Maps.newHashMap();
		
		protected final Map<String, TagTreeNode> childTags = Maps.newHashMap();
		private final TagTreeNode parent;
		private String name;
		
		/**
		 * Whether this tag is active.  A tag that is inactive does not load
		 * child tags or tokens.
		 */
		private boolean isActive = true;
		
		private static final String TAG = "TagTreeNode";
		
		public TagTreeNode(TagTreeNode parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		public TagTreeNode getOrAddChildTag(String tag) {
			if (!childTags.containsKey(tag)) {
				TagTreeNode node = new TagTreeNode(this, tag);
				childTags.put(tag, node);
				Log.v(TAG, "Adding child tag: " + tag + " to " + name);
				Log.v(TAG, Integer.toString(childTags.size()));
				return node;
			} else {
				return childTags.get(tag);
			}
		}

        /**
         * Renames this tag.
         * @param name The new name of the tag.
         * @return True on success, false if the tag could not be renamed.
         */
        public void rename(String name) {
            if (this.isSystemTag() || this.getParent() == null) return;

            this.getParent().childTags.remove(this.name);
            this.name = name;
            this.getParent().childTags.put(this.name, this);
        }
		
		public Collection<String> getImmediateTokens() {
			return this.isActive ? tokenCounts.keySet() : new ArrayList<String>();
		}
		
		/**
		 * Gets all tokens in this tag and all child tags.
		 * Will not return tokens from disabled tags, unless that token appears in
		 * another non-system tag.
		 * Tokens from system tags will only appear if they have not been explicitly
		 * disabled.
		 * @return A set of all TokenIDs in this subtree of tags.
		 */
		public Set<String> getAllTokens() {
			if (!this.isActive) {
				return Sets.newHashSet();
			}
			
			// If this is a system tag, we also want to get the excluded tokens from non-system tags
			// from the root.
			if (this.isSystemTag()) {
				Set<String> parentResult = Sets.newHashSet();
				Set<String> parentExcludedTokens = Sets.newHashSet();
				
				TagTreeNode rootNode = this;
				while (rootNode.getParent() != null) {
					rootNode = rootNode.getParent();
				}
				
				rootNode.getAllTokensHelper(parentResult, parentExcludedTokens, true, false);
				parentExcludedTokens.removeAll(parentResult);
				return Sets.difference(this.tokenCounts.keySet(), parentExcludedTokens);
			} else {
				Set<String> result = Sets.newHashSet();
				Set<String> excludedTokens = Sets.newHashSet();
				this.getAllTokensHelper(result, excludedTokens, true, false);
				excludedTokens.removeAll(result);
				
				// Find the system tags separately, and include ONLY those system tags that haven't
				// been explicit excluded.
				Set<String> tokensFromSystemTags = Sets.newHashSet();
				this.getAllTokensHelper(tokensFromSystemTags, null, true, true);
				
				result.addAll(Sets.difference(tokensFromSystemTags, excludedTokens));
				return result;
			}
		}
		
		private void getAllTokensHelper(
                Collection<String> result, Collection<String> excludedTokens,
                boolean respectExclusion, boolean systemTags) {
			if (!this.isActive && respectExclusion) {
				if (excludedTokens != null) {
					getAllTokensHelper(excludedTokens, null, false, systemTags);
				}
				return;
			}
			
			if (this.isSystemTag() == systemTags && (this.isActive || !respectExclusion)) {
				result.addAll(this.tokenCounts.keySet());
			}
			
			for (TagTreeNode n: this.childTags.values()) {
				n.getAllTokensHelper(result, excludedTokens, respectExclusion, systemTags);
			}
		}
		
		public TagTreeNode getNamedChild(String tagPath, boolean createTags) {
			TagTreeNode current = this;
			for (String s: tagPath.split(":")) {
				if (createTags) {
					current = current.getOrAddChildTag(s);
				} else {
					// TODO: something sane if tag doesn't exist.
					current = current.childTags.get(s);
				}
			}
			return current;
		}
		
		public Collection<String> getTagNames() {
			return this.isActive ? this.childTags.keySet() : new ArrayList<String>();
		}

		public boolean hasChildren() {
			return this.childTags.size() != 0 && this.isActive;
		}

		public void deleteToken(String tokenId) {
			this.tokenCounts.remove(tokenId);
			for (TagTreeNode childTag : this.childTags.values()) {
				childTag.deleteToken(tokenId);
				// TODO: Do we need to remove the childTag if it is now empty?
			}
			
			// Clean up guest token counts in parent tags.
			// NOTE: This could cause some unexpected behavior if two child tags
			// have the same token in them.
			TagTreeNode parent = this.parent;
			while (parent != null) {
				parent.guestTokenCounts.remove(tokenId);
				parent = parent.parent;
			}
		}

		public void deleteSelf() {
			this.parent.childTags.remove(this.name);
		}
		
		public Element toXml(Document document) {
			Element el = document.createElement("tag");
			el.setAttribute("name", this.name);
			el.setAttribute("active", Boolean.toString(this.isActive));
			for (Entry<String, Integer> tokenCount: this.tokenCounts.entrySet()) {
				Element tokenEl = document.createElement("token");
				tokenEl.setAttribute("name", tokenCount.getKey());
				tokenEl.setAttribute("count", Integer.toString(tokenCount.getValue()));
				el.appendChild(tokenEl);
			}
			for (Entry<String, Integer> guestCount: this.guestTokenCounts.entrySet()) {
				Element guestCountEl = document.createElement("guest_count");
				guestCountEl.setAttribute("name", guestCount.getKey());
				guestCountEl.setAttribute("count", Integer.toString(guestCount.getValue()));
				el.appendChild(guestCountEl);
			}
			
			for (TagTreeNode treeNode: this.childTags.values()) {
				el.appendChild(treeNode.toXml(document));
			}
			
			return el;
		}

		public void addToken(String tokenId) {
			this.tokenCounts.put(tokenId, 1);
			Log.v(TAG, "Adding token: " + tokenId + " to " + name);
		}
		
		public void setTokenCount(String tokenId, int count) {
			if (this.tokenCounts.containsKey(tokenId)) {
				this.tokenCounts.put(tokenId, count);
			} else {
				this.guestTokenCounts.put(tokenId, count);
			}
		}
		
		public int getTokenCount(String tokenId) {
			if (this.tokenCounts.containsKey(tokenId)) {
				return this.tokenCounts.get(tokenId);
			} else if (this.guestTokenCounts.containsKey(tokenId)) {
				return this.guestTokenCounts.get(tokenId);
			} else {
				// Try to get a count from the child tags
				for (TagTreeNode treeNode: this.childTags.values()) {
					int cnt = treeNode.getTokenCount(tokenId);
					if (cnt != 0) {
						return cnt;
					}
				}
			}
			return 0;
		}

		public TagTreeNode getParent() {
			return this.parent;
		}

		public String getName() {
			return this.name;
		}
		
		public boolean isActive() {
			return this.isActive;
		}
		
		public void setIsActive(boolean active) {
			this.isActive = active;
		}

		public String getPath() {
			TagTreeNode current = this;
			String result = this.getName();
			while (current.parent != null) {
				current = current.parent;
				if (current.parent != null) { // Don't get "root" in there!
					result = current.name + ":" + result;
				}
			}
			return result;
		}
		
		public boolean isSystemTag() {
			return TokenDatabase.isSystemTag(this.getPath());
		}

		public TagTreeNode createLimitedChild(String tagName, int maxSize) {
			if (!childTags.containsKey(tagName)) {
				TagTreeNode node = new LimitedTagTreeNode(this, tagName, maxSize);
				childTags.put(tagName, node);
				return node;
			} else {
				return childTags.get(tagName);
			}
		}
	}
	
	/**
	 * Extends TagTreeNode to limit the number of tags in the node.  Tags are
	 * stored in a LIFO queue.  The queue order persists across app runs.
	 * @author Tim
	 *
	 */
	protected class LimitedTagTreeNode extends TagTreeNode {

		private final int maxSize;
		private int nextAge;
		private final Map<String, Integer> nodeAges = Maps.newHashMap();
		
		public LimitedTagTreeNode(TagTreeNode parent, String name, int maxSize) {
			super(parent, name);
			this.maxSize = maxSize;
		}
		
		public void addToken(String tokenId) {
			addToken(tokenId, nextAge++);
		}
		
		public void addToken(String tokenId, int age) {
			if (nodeAges.size() == maxSize) {
				this.deleteToken(this.getOldestToken());
			}
			super.addToken(tokenId);
			nodeAges.put(tokenId, age);
			nextAge = Math.max(age + 1, nextAge);
		}
		
		public void deleteToken(String tokenId) {
			super.deleteToken(tokenId);
			nodeAges.remove(tokenId);
		}

		private String getOldestToken() {
			String oldestToken = null;
			int age = Integer.MAX_VALUE;
			for (Entry<String, Integer> entry: this.nodeAges.entrySet()) {
				if (entry.getValue() < age) {
					age = entry.getValue();
					oldestToken = entry.getKey();
				}
			}
			return oldestToken;
		}
		
		public Element toXml(Document document) {
			Element el = document.createElement("limited_tag");
			el.setAttribute("name", this.getName());
			el.setAttribute("active", Boolean.toString(this.isActive()));
			el.setAttribute("maxSize", Integer.toString(this.maxSize));
			for (String tokenId: this.getImmediateTokens()) {
				Element tokenEl = document.createElement("token");
				tokenEl.setAttribute("name", tokenId);
				tokenEl.setAttribute("age", nodeAges.get(tokenId).toString());
				el.appendChild(tokenEl);
			}
			
			for (TagTreeNode treeNode: this.childTags.values()) {
				el.appendChild(treeNode.toXml(document));
			}
			
			return el;
		}
	}

    /**
     * Always-present member at the top of the tag list that selects all tokens.
     */
    public static final String ALL = "All";

	public static final String RECENTLY_ADDED = "recently added";

	private static final int RECENTLY_ADDED_LIMIT = 20;

    public static final String RECENTLY_USED = "recently used";

    private static final int RECENTLY_USED_LIMIT = 20;

    public static final String RECENTLY_REMOVED = "recently killed";

    private static final int RECENTLY_REMOVED_LIMIT = 20;

    private static final Set<String> SYSTEM_TAG_NAMES = Sets.newHashSet(
            "built-in", "custom", "image", "letter", "solid color", RECENTLY_ADDED,
            RECENTLY_USED, RECENTLY_REMOVED, "artist");

    /**
     * The singleton token database instance.
     */
    private static TokenDatabase instance;

    /**
     * Built in tokens that have been deleted.
     */
    private final Set<String> mDeletedBuiltInTokens = Sets.newHashSet();

    /**
     * Mapping from deprecated token IDs to their replacements.
     */
    private final Map<String, String> mOldIdMapping = Maps.newHashMap();

    /**
     * Whether tags need to be pre-populated during the loading step. By default
     * we want this to be true (so that tokens added to the library get the
     * right tags), but we want to be able to suppress it while batch loading
     * tokens.
     */
    private transient boolean mPrePopulateTags = true;

    /**
     * Mapping from a Token ID to an instantiated token object that has that ID.
     */
    private final transient Map<String, BaseToken> mTokenForId = Maps.newHashMap();
    
    private final transient TagTreeNode mTagTreeRoot = new TagTreeNode(null, TokenDatabase.ALL);

    /**
     * Returns the instance of the token database.
     * 
     * @param context
     *            A context to use when loading data if needed.
     * @return The token database.
     */
    public static TokenDatabase getInstance(final Context context, boolean forceReload) {
        if (instance == null || forceReload) {
            try {
                instance = TokenDatabase.load(context);
            } catch (Exception e) {
            	e.printStackTrace();
                instance = new TokenDatabase();
                instance.populate(context);
            }
        }
        return instance;
    }
    
    public static TokenDatabase getInstance(final Context context) {
    	return getInstance(context, false);
    }

    /**
     * Returns an instance of the token database, or null if it hasn't been
     * created.
     * 
     * @return The token database.
     */
    public static TokenDatabase getInstanceOrNull() {
        return instance;
    }
    
    private static File databaseFile(Context context) {
    	return new File(context.getExternalFilesDir(null), "token_database.xml");
    }

    /**
     * Loads a token database from internal storage, and replaces the current
     * singleton token database with the loaded one.
     * 
     * @param context
     *            The context to use when loading.
     * @return The loaded database.
     * @throws IOException
     *             On read error.
     */
    private static TokenDatabase load(final Context context) throws IOException {
        TokenDatabase d = new TokenDatabase();
        d.populate(context);

        FileInputStream input = new FileInputStream(databaseFile(context));
        BufferedReader dataIn =
                new BufferedReader(new InputStreamReader(input));
        d.load(dataIn);
        dataIn.close();

        d.removeDeletedBuiltIns();
        
        return d;
    }

    /**
     * Private constructor - this is a singleton.
     */
    private TokenDatabase() {
    }

    /**
     * Adds a built-in image token with the given resource ID to the token
     * database.
     * 
     * @param resourceName 
     *            The name of the drawable resource to add.
     * @param resourceId
     *            The ID of the drawable resource to add.
     * @param sortOrder
     *            The sort order to use.
     * @param defaultTags
     *            Tags that this built in token should be in by default.
     */
    private void addBuiltin(final String resourceName, final int resourceId,
            final int sortOrder, Set<String> defaultTags) {
        BuiltInImageToken t =
                new BuiltInImageToken(resourceName, sortOrder, defaultTags);
        this.addTokenPrototype(t);
        this.mapOldId(t.getTokenId(),
                "BuiltInImageToken" + Integer.toString(resourceId));
    }

    /**
     * Adds a token and tags it with default tags.
     * 
     * @param token
     *            The token to add and tag.
     */
    public void addTokenPrototype(final BaseToken token) {
        this.mTokenForId.put(token.getTokenId(), token);
        if (this.mPrePopulateTags) {
            this.tagToken(token, token.getDefaultTags());
        }
    }

    /**
     * Creates a new token given the given token ID.
     * 
     * @param tokenId
     *            TokenID to create a token for.
     * @return A new token cloned from the prototype for that Token ID.
     */
    public BaseToken createToken(String tokenId) {
        tokenId = this.getNonDeprecatedTokenId(tokenId);
        BaseToken prototype = this.mTokenForId.get(tokenId);
        if (prototype == null) {
            Log.e("TokenDatabase", "Token did not exist for ID: " + tokenId);
            return new PlaceholderToken(tokenId);
        }
        try {
            return prototype.clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Could not create the token", e);
            return null;
        }
    }

    /**
     * Deletes the given tag from the database.
     * 
     * @param toDelete
     *            The tag to remove from the database.
     *
     * @return The parent node of the deleted node.
     */
    public TagTreeNode deleteTag(String toDelete) {
    	TagTreeNode node = this.mTagTreeRoot.getNamedChild(toDelete, false);
    	TagTreeNode parent = node.getParent();
        node.deleteSelf();
        return parent;
    }

    /**
     * Gets all tokens in the collection, sorted by ID.
     * 
     * @return The tokens.
     */
    public List<BaseToken> getAllTokens() {
        return this.tokenIdsToTokens(this.mTagTreeRoot.getAllTokens());
    }

    /**
     * Gets a TokenId, dereferencing a deprecated ID into the new ID if
     * necessary.
     * 
     * @param tokenId
     *            The ID to possibly dereference.
     * @return The dereferenced TokenID.
     */
    private String getNonDeprecatedTokenId(String tokenId) {
        if (this.mOldIdMapping.containsKey(tokenId)) {
            return this.mOldIdMapping.get(tokenId);
        }
        return tokenId;
    }
    
    private static final String TAG = "TokenDatabase";

    /**
     * Gets a list of all root tags in the token collection, sorted alphabetically
     * and case-insensitively.
     * 
     * @return The sorted tags.
     */
    public List<String> getTags() {
        ArrayList<String> l =
                new ArrayList<String>(this.mTagTreeRoot.getTagNames());
        Log.v(TAG, "getTags returning " + l.size() + "tags:");
        Collections.sort(l, new Comparator<String>() {
            @Override
            public int compare(final String s1, final String s2) {
                return s1.toUpperCase().compareTo(s2.toUpperCase());
            }
        });
        for (String s: l) {
        	Log.v(TAG, s);
        }
        return l;
    }

    /**
     * Given a tag, returns a sorted list of all tokens that have that tag.
     * 
     * @param tag
     *            The tag to look for.
     * @return The tokens associated with the requested tag.
     */
    public List<BaseToken> getTokensForTag(final String tag) {
        if (tag.equals(ALL)) {
            return this.getAllTokens();
        }
        if (this.mTagTreeRoot == null) return new ArrayList<>();

        TagTreeNode node = this.mTagTreeRoot.getNamedChild(tag, false);
        if (node == null) return new ArrayList<>();

        Set<String> tokenIds = node.getAllTokens();
        return this.tokenIdsToTokens(tokenIds);
    }

    /**
     * Reads data into this token database from the given reader.
     * 
     * @param dataIn
     *            Reader to get input from.
     * @throws IOException
     *             On read error.
     */
    private void load(final BufferedReader dataIn) {
        Log.w(TAG, "Loading token database from file");
    	try {
	        SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	        XMLReader xr = sp.getXMLReader();
	        xr.setContentHandler(new DatabaseReadHandler(this));
	        xr.parse(new InputSource(dataIn));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        
    }

    /**
     * Populates the token database with built-in image tokens by loading the
     * list of token resource names from the art credits.
     * 
     * @param context
     *            Context to load resources from.
     */
    private void loadBuiltInImageTokens(Context context) {
        try {
            InputStream is =
                    context.getResources().openRawResource(R.raw.art_credits);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(new ArtCreditHandler(context));
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
    }

    /**
     * Populates the database with solid-colored tokens.
     */
    private void loadColorTokens() {
        int sortOrder = 0;
        for (int color : Util.getStandardColorPalette()) {
            this.addTokenPrototype(new SolidColorToken(color, sortOrder));
            sortOrder += 1;
        }
    }

    /**
     * Searches for tokens that display images from the custom image directory
     * on the SD card and adds them to the database.
     * 
     * @param dataManager
     *            The data manager to use when searching for tokens.
     */
    private void loadCustomImageTokens(final DataManager dataManager) {
        CustomBitmapToken.registerDataManager(dataManager);
        for (String filename : dataManager.tokenFiles()) {
            this.addTokenPrototype(new CustomBitmapToken(filename));
        }
    }

    /**
     * Populates the database with letter-in-a-circle tokens.
     */
    private void loadLetterTokens() {
        this.addTokenPrototype(new LetterToken("A"));
        this.addTokenPrototype(new LetterToken("B"));
        this.addTokenPrototype(new LetterToken("C"));
        this.addTokenPrototype(new LetterToken("D"));
        this.addTokenPrototype(new LetterToken("E"));
        this.addTokenPrototype(new LetterToken("F"));
        this.addTokenPrototype(new LetterToken("G"));
        this.addTokenPrototype(new LetterToken("H"));
        this.addTokenPrototype(new LetterToken("I"));
        this.addTokenPrototype(new LetterToken("J"));
        this.addTokenPrototype(new LetterToken("K"));
        this.addTokenPrototype(new LetterToken("L"));
        this.addTokenPrototype(new LetterToken("M"));
        this.addTokenPrototype(new LetterToken("N"));
        this.addTokenPrototype(new LetterToken("O"));
        this.addTokenPrototype(new LetterToken("P"));
        this.addTokenPrototype(new LetterToken("Q"));
        this.addTokenPrototype(new LetterToken("R"));
        this.addTokenPrototype(new LetterToken("S"));
        this.addTokenPrototype(new LetterToken("T"));
        this.addTokenPrototype(new LetterToken("U"));
        this.addTokenPrototype(new LetterToken("V"));
        this.addTokenPrototype(new LetterToken("W"));
        this.addTokenPrototype(new LetterToken("X"));
        this.addTokenPrototype(new LetterToken("Y"));
        this.addTokenPrototype(new LetterToken("Z"));
    }

    /**
     * Adds a note that the given old ID is deprecated in favor of the given new
     * ID.
     * 
     * @param newId
     *            The new ID.
     * @param oldId
     *            The deprecated ID.
     */
    private void mapOldId(String newId, String oldId) {
        this.mOldIdMapping.put(oldId, newId);
    }

    /**
     * Populates the token database with built-in tokens and custom tokens
     * loaded from the token manager.
     * 
     * @param context
     *            Application context that manages the resources loaded by this
     *            database.
     */
    public void populate(Context context) {
        Log.w(TAG, "Populating token database defaults.");
        this.mPrePopulateTags = !this.tagsLoaded();
        this.loadCustomImageTokens(new DataManager(context));
        this.loadBuiltInImageTokens(context);
        this.loadColorTokens();
        this.loadLetterTokens();
        
        // Create the "recently added/used/removed" tags.
        this.mTagTreeRoot.createLimitedChild(RECENTLY_ADDED, RECENTLY_ADDED_LIMIT);
        this.mTagTreeRoot.createLimitedChild(RECENTLY_USED, RECENTLY_USED_LIMIT);
        this.mTagTreeRoot.createLimitedChild(RECENTLY_REMOVED, RECENTLY_REMOVED_LIMIT);

        this.mPrePopulateTags = false;
    }

    /**
     * Remove all built-in tokens that the user has previously deleted.
     */
    private void removeDeletedBuiltIns() {
        for (String removedBuiltin : this.mDeletedBuiltInTokens) {
            this.mTokenForId.remove(removedBuiltin);
        }
    }

    /**
     * Removes the given tag from the given token. It is not an error to remove
     * a tag that is not on the token in the first place.
     * 
     * @param tokenId
     *            ID of the token to remove the tag from.
     * @param tag
     *            The tag to remove.
     */
    public void removeTagFromToken(String tokenId, final String tag) {
        tokenId = this.getNonDeprecatedTokenId(tokenId);
        this.mTagTreeRoot.getNamedChild(tag, false).deleteToken(tokenId);

    }

    /**
     * Removes a token from the database. This does nothing to ensure that the
     * token will not be re-added next time the database populates.
     * 
     * @param token
     *            The token to remove.
     */
    public void removeToken(final BaseToken token) {
        this.mTagTreeRoot.deleteToken(token.getTokenId());
        this.mTokenForId.remove(token.getTokenId());
        if (token.isBuiltIn()) {
            this.mDeletedBuiltInTokens.add(token.getTokenId());
        }
    }

    /**
     * Writes the token database to the given writer.
     * 
     * @param output
     *            The writer to write the token database to.
     * @throws IOException
     *             on write error.
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     */
    private void save(final BufferedWriter output)
            throws ParserConfigurationException, TransformerException {
    	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		Element root = doc.createElement("token_database");
		doc.appendChild(root);
		
		
    	// Write out deleted tokens
		for (String tokenName : this.mDeletedBuiltInTokens) {
			Element deletedTokenEl = doc.createElement("deleted_builtin_token");
			root.appendChild(deletedTokenEl);
			deletedTokenEl.setAttribute("name", tokenName);
		}
    	
    	// Write out the token tag tree
		for (TagTreeNode node: this.mTagTreeRoot.childTags.values()) {
			root.appendChild(node.toXml(doc));
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		
		StreamResult result = new StreamResult(output);
		transformer.transform(source, result);
    }

    /**
     * Saves the token database to internal storage.
     * 
     * @param context
     *            Context to use when saving the database.
     * @throws IOException
     *             on write error.
     * @throws ParserConfigurationException 
     * @throws TransformerException 
     */
    public void save(final Context context) throws IOException, ParserConfigurationException, TransformerException {
        FileOutputStream output = new FileOutputStream(databaseFile(context));
        BufferedWriter dataOut =
                new BufferedWriter(new OutputStreamWriter(output));
        this.save(dataOut);
        dataOut.close();
    }

    /**
     * @return True if tags have already been loaded from the file, false if
     *         they need to be pre-populated.
     */
    private boolean tagsLoaded() {
        return this.mTagTreeRoot.hasChildren();
    }

    /**
     * Tags the token with the given collection of tags.
     * 
     * @param token
     *            The token object to tag.
     * @param tags
     *            Tags to add to the token.
     */
    public void tagToken(final BaseToken token, final Set<String> tags) {
        this.tagToken(token.getTokenId(), tags);
    }

    /**
     * Tags the token with the given collection of tags.
     * 
     * @param tokenId
     *            ID of the token to tag.
     * @param tags
     *            Tags to add to the token.
     */
    public void tagToken(String tokenId, final Collection<String> tags) {
        tokenId = this.getNonDeprecatedTokenId(tokenId);
        for (String tag : tags) {
        	this.mTagTreeRoot.getNamedChild(tag, true).addToken(tokenId);
        }
    }

    /**
     * Tags the token with the given single tag.
     * 
     * @param tokenId
     *            The ID of the token object to tag.
     * @param tag
     *            The tag to add.
     */
    public void tagToken(final String tokenId, final String tag) {
        Set<String> tags = new HashSet<String>();
        tags.add(tag);
        this.tagToken(tokenId, tags);

    }

    /**
     * Given a collection of token IDs, returns a list of tokens, sorted based
     * on the sort order that each token class defines, that that contains the
     * known tokens that match those IDs.
     * 
     * @param tokenIds
     *            Collection of IDs to look up.
     * @return List of tokens.
     */
    private List<BaseToken> tokenIdsToTokens(final Collection<String> tokenIds) {

        List<BaseToken> tokens = new ArrayList<BaseToken>();

        for (String tokenId : tokenIds) {
            tokenId = this.getNonDeprecatedTokenId(tokenId);
            // Add the token for this ID.
            // No worries if the token doesn't exist - by design the database
            // could include tokens that don't exist anymore since it connects a
            // loaded token id to stored information about that ID.
            if (this.mTokenForId.containsKey(tokenId)) {
                tokens.add(this.mTokenForId.get(tokenId));
            }
        }

        Collections.sort(tokens, new Comparator<BaseToken>() {
            @Override
            public int compare(BaseToken t1, BaseToken t2) {
                // TODO Auto-generated method stub
                return t1.getSortOrder().compareTo(t2.getSortOrder());
            }
        });
        return tokens;
    }
    
    private class DatabaseReadHandler extends DefaultHandler {
    	
    	/**
    	 * The TokenDatabase instance to read data into.
    	 */
    	private final TokenDatabase database;
    	
    	/**
    	 * Argh I fucking hate having to emulate recursion.  But I need to in
    	 * this case.
    	 */
    	private TagTreeNode currentTagTreeNode = null;
    	
    	public DatabaseReadHandler(TokenDatabase d) {
    		database = d;
    		currentTagTreeNode = d.mTagTreeRoot;
    	}
    	private static final String TAG = "DatabaseReadHandler";
    	public void startElement(String namespaceURI, String localName,
                String qName, org.xml.sax.Attributes attributes) {
    		if (localName.equalsIgnoreCase("deleted_builtin_token")) {
    			String tokenName = attributes.getValue("name");
    			Log.v(TAG, "PROCESS DELETED TOKEN: " + tokenName);
    			database.mDeletedBuiltInTokens.add(tokenName);
    		} else if (localName.equalsIgnoreCase("tag")) {
    			String tagName = attributes.getValue("name");
    			String active = attributes.getValue("active");
    			boolean isActive = active == null || Boolean.parseBoolean(active);
    			Log.v(TAG, "START TAG: " + tagName);
    			currentTagTreeNode = currentTagTreeNode.getNamedChild(tagName, true);
    			currentTagTreeNode.setIsActive(isActive);
    		} else if (localName.equalsIgnoreCase("limited_tag")) {
    			String tagName = attributes.getValue("name");
    			int maxSize = Integer.parseInt(attributes.getValue("maxSize"));
    			String active = attributes.getValue("active");
    			boolean isActive = active == null || Boolean.parseBoolean(active);
    			Log.v(TAG, "START TAG: " + tagName);
    			currentTagTreeNode = currentTagTreeNode.createLimitedChild(tagName, maxSize);
    			currentTagTreeNode.setIsActive(isActive);
    		} else if (localName.equalsIgnoreCase("token")) {
    			String tokenName = attributes.getValue("name");
    			String age = attributes.getValue("age");
    			String countStr = attributes.getValue("count");
    			int count;
    			if (countStr != null) {
    				count = Integer.parseInt(countStr);
    			} else {
    				count = 1;
    			}
    			Log.v(TAG, "ADD TOKEN " + tokenName + " TO TAG " + currentTagTreeNode.name);
    			if (age != null) {
    				((LimitedTagTreeNode)currentTagTreeNode).addToken(tokenName, Integer.parseInt(age));
    			} else {
    				currentTagTreeNode.addToken(tokenName);
    			}
    			currentTagTreeNode.setTokenCount(tokenName, count);
    		} else if (localName.equalsIgnoreCase("guest_count")) {
    			String tokenName = attributes.getValue("name");
    			String countStr = attributes.getValue("count");
    			int count;
    			if (countStr != null) {
    				count = Integer.parseInt(countStr);
    			} else {
    				count = 1;
    			}
    			currentTagTreeNode.setTokenCount(tokenName, count);
    		}
    	}
    	
    	public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) {
    		if (localName.equalsIgnoreCase("tag") || localName.equalsIgnoreCase("limited_tag")) {
    			Log.v(TAG, "LEAVE TAG: " + currentTagTreeNode.name);
    			currentTagTreeNode = currentTagTreeNode.parent;
    		}
        }
    }
    
    

    /**
     * SAX handler to load the resources that represent built-in tokens from the
     * art credits file.
     * 
     * @author Tim
     * 
     */
    private class ArtCreditHandler extends DefaultHandler {
        /**
         * Application context to load resources from.
         */
        private final Context mContext;

        /**
         * Count of the tokens that have been loaded; used to sort them later.
         */
        private int mCurrentSortOrder = 0;

        /**
         * Constructor.
         * 
         * @param context
         *            Application context to load resources from.
         */
        public ArtCreditHandler(Context context) {
            this.mContext = context;
        }
        private String currentArtist;
        @Override
        public void startElement(String namespaceURI, String localName,
                String qName, org.xml.sax.Attributes attributes) throws SAXException {
        	
            // Possibly limit the number of built-in tokens loaded, for debug
            // purposes.
            //noinspection PointlessArithmeticExpression
            if (mCurrentSortOrder > DeveloperMode.MAX_BUILTIN_TOKENS) {
                return;
            }

            if (localName.equalsIgnoreCase("artist")) {
            	currentArtist = attributes.getValue("name");
            }
            else if (localName.equalsIgnoreCase("token")) {
                int id =
                        this.mContext.getResources().getIdentifier(
                                attributes.getValue("res"), "drawable",
                                mContext.getPackageName());
                if (id == 0) {
                    Log.e("TokenDatabase",
                            "Image resource for name='" + attributes.getValue("res")
                            + "' not found in database");
                    return;
                }
                String tagList = attributes.getValue("tags");
                Set<String> defaultTags = Sets.newHashSet();
                if (tagList != null) {
                    Collections.addAll(defaultTags, tagList.split(","));
                }
                defaultTags.add("artist:" + currentArtist);
                TokenDatabase.this.addBuiltin(attributes.getValue("res"), id,
                        this.mCurrentSortOrder, defaultTags);
                this.mCurrentSortOrder++;
            }
        }
    }

	public TagTreeNode getRootNode() {
		return this.mTagTreeRoot;
	}

	public void setTokenTagCount(String tokenId, String tag, int count) {
		this.getRootNode().getNamedChild(tag, false).setTokenCount(tokenId, count);
	}

    /**
     * Changes the name of the given tag.
     * @param tagPath The full path to the tag to rename.
     * @param newName The new name of the last tag on the path.
     * @return The modified path of the tag.
     */
    public String renameTag(String tagPath, String newName) {
        TagTreeNode n = this.getRootNode().getNamedChild(tagPath, false);
        n.rename(newName);
        return n.getPath();
    }

    /**
     * Restores all previously deleted tokens to the token library.  Newly added tokens will still
     * be present.
     *
     * Note that this will read the art credits file to restore tags, so it should ideally be done
     * off the main thread.
     */
    public void restoreDefaults(Context context) {
        this.mDeletedBuiltInTokens.clear();
        this.loadBuiltInImageTokens(context);
    }
}
