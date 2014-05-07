package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tbocek.android.combatmap.model.primitives.BaseToken;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.dungeonsketch.R;

public class TokenDeploymentLineItem extends LinearLayout {

	private Button mDecreaseButton;
    private TextView mAmountTextView;
	private ImageView mTokenImageView;

	private int mNumberToDeploy;

	private BaseToken mToken;

	
	public TokenDeploymentLineItem(Context context) {
		super(context);
		LayoutInflater.from(context).inflate(R.layout.token_deployment_line_item, this);
	
		mDecreaseButton = (Button) this.findViewById(R.id.token_deployment_button_decrease);
		mAmountTextView = (TextView) this.findViewById(R.id.token_deployment_number);
		mTokenImageView = (ImageView) this.findViewById(R.id.token_deployment_image);
		
		mDecreaseButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onDecrementButtonClick();
			}
		});

        Button increaseButton = (Button) this.findViewById(R.id.token_deployment_button_increase);
		increaseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onIncrementButtonClick();
            }
        });
	}
	
	private void onIncrementButtonClick() {
		mNumberToDeploy++;
		updateControls();
	}
	
	private void onDecrementButtonClick() {
		if (mNumberToDeploy > 0) {
			mNumberToDeploy--;
			updateControls();
		}
	}
	
	private void updateControls() {
		this.mAmountTextView.setText(Integer.toString(mNumberToDeploy));
		this.mDecreaseButton.setEnabled(mNumberToDeploy > 0);
	}
	
	public int getNumberToDeploy() {
		return mNumberToDeploy;
	}

	public void setNumberToDeploy(int numberToDeploy) {
		mNumberToDeploy = numberToDeploy;
		updateControls();
	}

	public BaseToken getToken() {
		return mToken;
	}

	public void setToken(BaseToken token) {
		this.mToken = token;
		createTokenImage();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		createTokenImage();
	}

	private void createTokenImage() {
		int width = (int) Util.convertDpToPixel(48, this.getContext());
		int height = (int) Util.convertDpToPixel(48, this.getContext());
		
		if (width > 0 && height > 0) {
			Bitmap tokenImage = Bitmap.createBitmap(
					width, height,
					Bitmap.Config.ARGB_8888);
			
			Canvas c = new Canvas(tokenImage);
			mToken.draw(c, width / 2, height / 2, Math.min(width, height) / 2, true, true);
			
			mTokenImageView.setImageBitmap(tokenImage);
		}
	}
}
