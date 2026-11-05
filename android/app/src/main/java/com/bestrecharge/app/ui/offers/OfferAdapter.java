package com.bestrecharge.app.ui.offers;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.databinding.ItemOfferCardBinding;

import java.util.ArrayList;
import java.util.List;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    public interface OnOfferClickListener {
        void onViewTerms(OfferEntity offer);
        void onMarkUsed(OfferEntity offer);
    }

    private List<OfferEntity> offers = new ArrayList<>();
    private final OnOfferClickListener listener;

    public OfferAdapter(OnOfferClickListener listener) {
        this.listener = listener;
    }

    public void setOffers(List<OfferEntity> newOffers) {
        this.offers = newOffers != null ? newOffers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOfferCardBinding binding = ItemOfferCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new OfferViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        holder.bind(offers.get(position));
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    class OfferViewHolder extends RecyclerView.ViewHolder {
        private final ItemOfferCardBinding binding;

        public OfferViewHolder(ItemOfferCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OfferEntity offer) {
            binding.tvProviderBadge.setText(offer.provider != null ? offer.provider.toUpperCase() : "UPI");
            binding.tvEligibilityBadge.setText(offer.userEligibility != null ? offer.userEligibility.replace("_", " ") : "ALL");
            binding.tvVerificationStatus.setText(offer.verificationStatus != null ? offer.verificationStatus.replace("_", " ") : "Verified");
            binding.tvOfferTitle.setText(offer.title);
            binding.tvOfferDesc.setText(offer.description);
            binding.tvMinTxn.setText("Min Txn: ₹" + (int) offer.minTransactionAmount);
            binding.tvExpiry.setText("Expires: " + (offer.expiryDate != null ? offer.expiryDate : "Ongoing"));

            if ("USED".equalsIgnoreCase(offer.userTag)) {
                binding.btnMarkUsed.setText("Used ✓");
                binding.btnMarkUsed.setEnabled(false);
            } else {
                binding.btnMarkUsed.setText("Mark Used");
                binding.btnMarkUsed.setEnabled(true);
            }

            binding.btnViewTerms.setOnClickListener(v -> {
                if (listener != null) listener.onViewTerms(offer);
            });

            binding.btnMarkUsed.setOnClickListener(v -> {
                if (listener != null) listener.onMarkUsed(offer);
            });
        }
    }
}
