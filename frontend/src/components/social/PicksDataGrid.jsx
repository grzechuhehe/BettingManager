import React from 'react';
import {
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Chip,
    Box,
    Grid
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const getStatusColor = (status) => {
    switch (status) {
        case 'WON': return 'success';
        case 'LOST': return 'error';
        case 'VOID': return 'default';
        case 'PENDING': return 'primary';
        default: return 'default';
    }
};

export default function PicksDataGrid({ picks }) {
    if (!picks || picks.length === 0) {
        return (
            <Typography variant="body1" sx={{ color: 'gray', fontStyle: 'italic', mt: 2 }}>
                No AI-extracted picks found for this profile yet.
            </Typography>
        );
    }

    return (
        <Box sx={{ mt: 2 }}>
            {picks.map((pick) => (
                <Accordion key={pick.id} sx={{ mb: 1, backgroundColor: 'rgba(255, 255, 255, 0.05)', color: 'white', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
                    <AccordionSummary
                        expandIcon={<ExpandMoreIcon sx={{ color: 'white' }} />}
                        aria-controls={`panel${pick.id}-content`}
                        id={`panel${pick.id}-header`}
                    >
                        <Grid container alignItems="center" spacing={2}>
                            <Grid item xs={12} sm={5}>
                                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                                    {pick.eventName || 'Multiple Events (Parlay)'}
                                </Typography>
                            </Grid>
                            <Grid item xs={6} sm={4}>
                                <Typography variant="body2" sx={{ color: 'rgba(255, 255, 255, 0.7)' }}>
                                    {pick.selection || 'Parlay'}
                                </Typography>
                            </Grid>
                            <Grid item xs={6} sm={3} sx={{ textAlign: 'right' }}>
                                <Chip 
                                    label={pick.status || 'PENDING'} 
                                    size="small" 
                                    color={getStatusColor(pick.status)}
                                    sx={{ fontWeight: 'bold' }}
                                />
                            </Grid>
                        </Grid>
                    </AccordionSummary>
                    <AccordionDetails sx={{ borderTop: '1px solid rgba(255, 255, 255, 0.1)', backgroundColor: 'rgba(0, 0, 0, 0.2)' }}>
                        <Grid container spacing={3}>
                            <Grid item xs={12} md={pick.legs && pick.legs.length > 0 ? 4 : 12}>
                                <Box sx={{ p: 1 }}>
                                    <Typography variant="body2" gutterBottom>
                                        <strong>Total Odds:</strong> {pick.odds}
                                    </Typography>
                                    <Typography variant="body2" gutterBottom>
                                        <strong>Units:</strong> {pick.units}
                                    </Typography>
                                    <Typography variant="body2" gutterBottom>
                                        <strong>Bookmaker:</strong> {pick.bookmaker || 'Unknown'}
                                    </Typography>
                                    <Typography variant="body2" gutterBottom>
                                        <strong>Placed At:</strong> {new Date(pick.placedAt).toLocaleString()}
                                    </Typography>
                                    {pick.imageProofPath && (
                                        <Box sx={{ mt: 2 }}>
                                            <Typography variant="caption" display="block" gutterBottom sx={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                                                Proof Image:
                                            </Typography>
                                            <img 
                                                src={`https://localhost:8443${pick.imageProofPath}`} 
                                                alt="Bet slip proof" 
                                                style={{ maxWidth: '100%', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.1)' }}
                                                onError={(e) => e.target.style.display = 'none'}
                                            />
                                        </Box>
                                    )}
                                </Box>
                            </Grid>
                            
                            {pick.legs && pick.legs.length > 0 && (
                                <Grid item xs={12} md={8}>
                                    <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold' }}>
                                        Parlay Legs
                                    </Typography>
                                    <TableContainer component={Paper} sx={{ backgroundColor: 'rgba(255, 255, 255, 0.03)', backgroundImage: 'none' }}>
                                        <Table size="small">
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell sx={{ color: 'rgba(255, 255, 255, 0.7)', fontWeight: 'bold' }}>Event</TableCell>
                                                    <TableCell sx={{ color: 'rgba(255, 255, 255, 0.7)', fontWeight: 'bold' }}>Selection</TableCell>
                                                    <TableCell sx={{ color: 'rgba(255, 255, 255, 0.7)', fontWeight: 'bold' }}>Odds</TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {pick.legs.map((leg, index) => (
                                                    <TableRow key={index}>
                                                        <TableCell sx={{ color: 'white', borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>{leg.eventName}</TableCell>
                                                        <TableCell sx={{ color: 'white', borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>{leg.selection}</TableCell>
                                                        <TableCell sx={{ color: 'white', borderBottom: '1px solid rgba(255, 255, 255, 0.05)' }}>{leg.odds}</TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                </Grid>
                            )}
                        </Grid>
                    </AccordionDetails>
                </Accordion>
            ))}
        </Box>
    );
}
