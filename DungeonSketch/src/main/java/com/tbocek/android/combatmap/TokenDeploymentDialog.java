package com.tbocek.android.combatmap;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.common.collect.Lists;
import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.view.TokenDeploymentLineItem;
import com.tbocek.dungeonsketch.R;

import java.util.List;

public class TokenDeploymentDialog extends Dialog {
	
	private LinearLayout mLineItemLayout;

    private boolean mSuccessful = false;
	
	private List<TokenDeploymentLineItem> mLineItems = Lists.newArrayList();
	
	public class TokenNumberPair {
		private BaseToken token;
		private int count;
		public TokenNumberPair(BaseToken t, int n) {
			token = t;
			count = n;
		}
		public BaseToken getToken() {
			return token;
		}
		public int getCount() {
			return count;
		}
	}
	
	public TokenDeploymentDialog(Context context) {
		super(context);
		this.setTitle("Deploy Tokens");
		this.setContentView(R.layout.token_deployment_dialog);
		
		mLineItemLayout = (LinearLayout) this.findViewById(R.id.token_deployment_dialog_line_items);

        Button deployButton = (Button) this.findViewById(R.id.token_deployment_dialog_deploy);
		deployButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mSuccessful = true;
                TokenDeploymentDialog.this.dismiss();
            }
        });
	}
	
	public void setTag(TokenDatabase database, String tag) {
		TokenDatabase.TagTreeNode node = database.getRootNode().getNamedChild(tag, false);
		for (BaseToken t: database.getTokensForTag(tag)) {
			TokenDeploymentLineItem li = new TokenDeploymentLineItem(this.getContext());
			mLineItemLayout.addView(li);
			li.setToken(t);
			int deploymentCount = node.getTokenCount(t.getTokenId());
			li.setNumberToDeploy(deploymentCount >= 0 ? deploymentCount : 1);
			mLineItems.add(li);
		}
	}
	
	public List<TokenNumberPair> getDeploymentList() {
		List<TokenNumberPair> result = Lists.newArrayList();
		if (mSuccessful) {
			for (TokenDeploymentLineItem i: mLineItems) {
				if (i.getNumberToDeploy() > 0) {
					result.add(new TokenNumberPair(i.getToken(), i.getNumberToDeploy()));
				}
			}
		}
		return result;
	}
}
